package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crud_backend.entity.Course;
import com.example.crud_backend.entity.CourseSchedule;
import com.example.crud_backend.entity.Teacher;
import com.example.crud_backend.mapper.CourseMapper;
import com.example.crud_backend.mapper.CourseScheduleMapper;
import com.example.crud_backend.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminBasicController {

    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    // --- 教师管理 ---
    @GetMapping("/teacher/list")
    public Map<String, Object> listTeachers(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Teacher> p = teacherMapper.selectPage(new Page<>(page, size), null);
        return wrapResult(p);
    }

    @PostMapping("/teacher")
    public String addTeacher(@RequestBody Teacher teacher) {
        teacherMapper.insert(teacher);
        return "success";
    }

    // --- 课程元数据管理 ---
    @GetMapping("/course/list")
    public Map<String, Object> listCourses() {
        // 简单返回所有，实际项目可分页
        return wrapData(courseMapper.selectList(null));
    }

    @PostMapping("/course")
    public String addCourse(@RequestBody Course course) {
        courseMapper.insert(course);
        return "success";
    }

    // --- 排课管理 (发布课程到时间表) ---
    // 这里管理员需要指定：哪个老师，教哪个课，周几第几节，在哪里
    @PostMapping("/schedule")
    public String addSchedule(@RequestBody CourseSchedule schedule) {
        // 简单校验：同一老师同一时间不能有两门课 (实际逻辑更复杂)
        Long count = courseScheduleMapper.selectCount(new QueryWrapper<CourseSchedule>()
                .eq("teacher_id", schedule.getTeacherId())
                .eq("week_day", schedule.getWeekDay())
                .eq("section_start", schedule.getSectionStart()));

        if (count > 0)
            return "该老师该时段已有课程";

        courseScheduleMapper.insert(schedule);
        return "success";
    }

    // --- 辅助方法 ---
    private Map<String, Object> wrapResult(Page<?> page) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", page.getRecords());
        res.put("total", page.getTotal());
        return res;
    }

    private Map<String, Object> wrapData(Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", data);
        return res;
    }
}