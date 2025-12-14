package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crud_backend.entity.CourseSchedule;
import com.example.crud_backend.entity.StudentCourse;
import com.example.crud_backend.mapper.CourseScheduleMapper;
import com.example.crud_backend.mapper.StudentCourseMapper;
import com.example.crud_backend.mapper.TimetableMapper;
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

        // 检查是否已选
        Long count = studentCourseMapper.selectCount(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getScheduleId, scheduleId));

        if (count > 0) {
            res.put("code", 400);
            res.put("msg", "已选修该课程，请勿重复选择");
            return res;
        }

        // 检查容量
        CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId);
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

        // 删除选课记录
        studentCourseMapper.delete(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getScheduleId, scheduleId));

        // 减少人数
        CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId);
        if (schedule != null && schedule.getCurrentCount() > 0) {
            schedule.setCurrentCount(schedule.getCurrentCount() - 1);
            courseScheduleMapper.updateById(schedule);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "退课成功");
        return res;
    }
}