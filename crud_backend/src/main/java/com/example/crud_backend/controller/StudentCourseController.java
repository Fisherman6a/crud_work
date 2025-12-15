package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.crud_backend.entity.CourseSchedule;
import com.example.crud_backend.entity.StudentCourse;
import com.example.crud_backend.mapper.CourseMapper;
import com.example.crud_backend.mapper.CourseScheduleMapper;
import com.example.crud_backend.mapper.StudentCourseMapper;
import com.example.crud_backend.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student-course")
@CrossOrigin(origins = "*") // 允许前端跨域调用
public class StudentCourseController {

    @Autowired
    private StudentCourseMapper studentCourseMapper;
    @Autowired
    private CourseScheduleMapper scheduleMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private TeacherMapper teacherMapper;

    // === 管理员功能 ===

    // 获取某个学生的详细课表（返回 List<Map>，无 Result 包装）
    @GetMapping("/admin/list/{studentNumber}")
    public List<Map<String, Object>> getStudentTimetable(@PathVariable String studentNumber) {
        QueryWrapper<StudentCourse> scWrapper = new QueryWrapper<>();
        scWrapper.eq("student_id", studentNumber);
        List<StudentCourse> scList = studentCourseMapper.selectList(scWrapper);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (StudentCourse sc : scList) {
            CourseSchedule schedule = scheduleMapper.selectById(sc.getScheduleId());
            if (schedule != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("studentCourseId", sc.getId());
                map.put("scheduleId", schedule.getId());
                map.put("weekDay", schedule.getWeekDay());
                map.put("sectionStart", schedule.getSectionStart());
                map.put("sectionEnd", schedule.getSectionEnd());
                map.put("location", schedule.getLocation());
                map.put("courseName", courseMapper.selectById(schedule.getCourseId()).getName());
                map.put("teacherName", teacherMapper.selectById(schedule.getTeacherId()).getName());
                resultList.add(map);
            }
        }
        return resultList;
    }

    // 管理员给学生选课
    @PostMapping("/admin/add")
    public ResponseEntity<String> addCourseAdmin(@RequestBody Map<String, Object> payload) {
        String studentNumber = (String) payload.get("studentNumber");
        Long scheduleId = Long.valueOf(payload.get("scheduleId").toString());

        QueryWrapper<StudentCourse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentNumber).eq("schedule_id", scheduleId);
        if (studentCourseMapper.selectCount(queryWrapper) > 0) {
            return ResponseEntity.badRequest().body("该课程已存在");
        }

        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentNumber);
        sc.setScheduleId(scheduleId);
        studentCourseMapper.insert(sc);

        // 更新人数
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        schedule.setCurrentCount(schedule.getCurrentCount() + 1);
        scheduleMapper.updateById(schedule);

        return ResponseEntity.ok("Success");
    }

    // 管理员移除课程
    @PostMapping("/admin/remove")
    public ResponseEntity<String> removeCourseAdmin(@RequestBody Map<String, Object> payload) {
        String studentNumber = (String) payload.get("studentNumber");
        Long scheduleId = Long.valueOf(payload.get("scheduleId").toString());

        QueryWrapper<StudentCourse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentNumber).eq("schedule_id", scheduleId);
        studentCourseMapper.delete(queryWrapper);

        // 更新人数
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule.getCurrentCount() > 0) {
            schedule.setCurrentCount(schedule.getCurrentCount() - 1);
            scheduleMapper.updateById(schedule);
        }
        return ResponseEntity.ok("Success");
    }
}