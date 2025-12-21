package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crud_backend.entity.*;
import com.example.crud_backend.mapper.*;
import com.example.crud_backend.service.INotificationService;
import com.example.crud_backend.service.SmsService;
import com.example.crud_backend.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timetable")
@CrossOrigin(origins = "*")
public class TimetableController {

    @Autowired
    private TimetableMapper timetableMapper;

    @Autowired
    private StudentCourseMapper studentCourseMapper;

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private SmsService smsService;

    // 1. 获取某学生的课表 (学生看自己的，管理员查别人的)
    @GetMapping("/student/{studentId}")
    public Map<String, Object> getStudentTimetable(@PathVariable String studentId) {
        List<Map<String, Object>> list = timetableMapper.selectTimetableByStudentId(studentId);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", list);
        return res;
    }

    // 2. 获取所有开放选课的课程列表
    @GetMapping("/available")
    public Map<String, Object> getAvailableCourses() {
        List<Map<String, Object>> list = timetableMapper.selectAllScheduleDetails();
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", list);
        return res;
    }

    // 3. 学生选课 / 管理员给学生加课
    @PostMapping("/select")
    @Transactional
    public Map<String, Object> selectCourse(@RequestBody Map<String, Object> params) {
        String studentId = (String) params.get("studentId");
        Long scheduleId = Long.valueOf(params.get("scheduleId").toString());

        Map<String, Object> res = new HashMap<>();

        // 获取要选的课程排课信息
        CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            res.put("code", 400);
            res.put("msg", "课程不存在");
            return res;
        }

        // ===== 业务规则1: 检查是否已选该排课 =====
        Long count = studentCourseMapper.selectCount(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getScheduleId, scheduleId));

        if (count > 0) {
            res.put("code", 400);
            res.put("msg", "已选修该课程，请勿重复选择");
            return res;
        }

        // ===== 业务规则2: 检查是否已选同一课程的其他班级 =====
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, studentId));

        for (StudentCourse sc : studentCourses) {
            CourseSchedule existingSchedule = courseScheduleMapper.selectById(sc.getScheduleId());
            if (existingSchedule != null && existingSchedule.getCourseId().equals(schedule.getCourseId())) {
                Course course = courseMapper.selectById(schedule.getCourseId());
                res.put("code", 400);
                res.put("msg", "您已选修《" + course.getName() + "》课程的其他班级，不能重复选修同一课程");
                return res;
            }
        }

        // ===== 业务规则3: 检查时间冲突 =====
        for (StudentCourse sc : studentCourses) {
            CourseSchedule existingSchedule = courseScheduleMapper.selectById(sc.getScheduleId());
            if (existingSchedule != null) {
                // 检查是否同一天
                if (existingSchedule.getWeekDay().equals(schedule.getWeekDay())) {
                    // 检查时间段是否重叠
                    boolean timeConflict = !(existingSchedule.getSectionEnd() < schedule.getSectionStart() ||
                            existingSchedule.getSectionStart() > schedule.getSectionEnd());
                    if (timeConflict) {
                        Course conflictCourse = courseMapper.selectById(existingSchedule.getCourseId());
                        String dayName = getDayName(schedule.getWeekDay());
                        res.put("code", 400);
                        res.put("msg", String.format("时间冲突：%s第%d-%d节已有《%s》课程",
                                dayName, schedule.getSectionStart(), schedule.getSectionEnd(), conflictCourse.getName()));
                        return res;
                    }
                }
            }
        }

        // ===== 业务规则4: 检查容量 =====
        if (schedule.getCurrentCount() >= schedule.getMaxCapacity()) {
            res.put("code", 400);
            res.put("msg", "课程人数已满");
            return res;
        }

        // 执行选课
        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentId);
        sc.setScheduleId(scheduleId);
        sc.setCreateTime(LocalDateTime.now());
        studentCourseMapper.insert(sc);

        // 更新已选人数
        schedule.setCurrentCount(schedule.getCurrentCount() + 1);
        courseScheduleMapper.updateById(schedule);

        // === 新增：发送通知（站内信 + 短信） ===
        try {
            Student student = studentMapper.selectById(studentId);
            Course course = courseMapper.selectById(schedule.getCourseId());

            if (student != null && course != null) {
                // 1. 创建站内通知
                Notification notification = new Notification();
                notification.setUserId(studentId);
                notification.setType("SELECTION_SUCCESS");
                notification.setTitle("选课成功");
                notification.setContent(String.format("您已成功选修《%s》课程", course.getName()));
                notification.setCourseId(course.getId());
                notification.setScheduleId(scheduleId);
                notification.setIsRead(false);
                notification.setCreateTime(LocalDateTime.now());
                notificationService.createNotification(notification);

                // 2. 通过WebSocket推送实时消息
                WebSocketServer.sendInfo(studentId,
                    String.format("选课成功：您已成功选修《%s》课程", course.getName()));

                // 3. 发送短信通知（通过RabbitMQ异步处理）
                if (student.getPhone() != null && !student.getPhone().isEmpty()) {
                    smsService.sendCourseSelectionSms(
                        student.getPhone(),
                        student.getName(),
                        course.getName()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("选课通知发送失败: " + e.getMessage());
            // 不影响选课主流程
        }

        res.put("code", 200);
        res.put("msg", "选课成功");
        return res;
    }

    // 4. 退课 (管理员或学生)
    @PostMapping("/drop")
    @Transactional
    public Map<String, Object> dropCourse(@RequestBody Map<String, Object> params) {
        String studentId = (String) params.get("studentId");
        Long scheduleId = Long.valueOf(params.get("scheduleId").toString());

        // 获取课程信息（用于通知）
        CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId);
        Student student = studentMapper.selectById(studentId);
        Course course = null;
        if (schedule != null) {
            course = courseMapper.selectById(schedule.getCourseId());
        }

        // 删除选课记录
        studentCourseMapper.delete(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getScheduleId, scheduleId));

        // 减少人数
        if (schedule != null && schedule.getCurrentCount() > 0) {
            schedule.setCurrentCount(schedule.getCurrentCount() - 1);
            courseScheduleMapper.updateById(schedule);
        }

        // === 新增：发送退课通知 ===
        try {
            if (student != null && course != null) {
                // 1. 创建站内通知
                Notification notification = new Notification();
                notification.setUserId(studentId);
                notification.setType("COURSE_CHANGE");
                notification.setTitle("退课成功");
                notification.setContent(String.format("您已成功退选《%s》课程", course.getName()));
                notification.setCourseId(course.getId());
                notification.setScheduleId(scheduleId);
                notification.setIsRead(false);
                notification.setCreateTime(LocalDateTime.now());
                notificationService.createNotification(notification);

                // 2. 通过WebSocket推送实时消息
                WebSocketServer.sendInfo(studentId,
                    String.format("退课成功：您已成功退选《%s》课程", course.getName()));

                // 3. 发送短信通知（通过RabbitMQ异步处理）
                if (student.getPhone() != null && !student.getPhone().isEmpty()) {
                    smsService.sendCourseDropSms(
                        student.getPhone(),
                        student.getName(),
                        course.getName()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("退课通知发送失败: " + e.getMessage());
            // 不影响退课主流程
        }

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "退课成功");
        return res;
    }

    /**
     * 辅助方法: 将星期数字转换为中文
     */
    private String getDayName(Integer weekDay) {
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        if (weekDay == null || weekDay < 1 || weekDay > 7) {
            return "未知";
        }
        return dayNames[weekDay];
    }
}