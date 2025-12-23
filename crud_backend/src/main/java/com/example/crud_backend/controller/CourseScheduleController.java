package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.crud_backend.entity.CourseSchedule;
import com.example.crud_backend.mapper.CourseMapper;
import com.example.crud_backend.mapper.CourseScheduleMapper;
import com.example.crud_backend.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedule")
public class CourseScheduleController {

    @Autowired
    private CourseScheduleMapper scheduleMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private TeacherMapper teacherMapper;

    // 1. 获取所有排课（带课程名和教师名，用于前端表格显示）
    @GetMapping("/listDetailed")
    public List<Map<String, Object>> listDetailed() {
        List<CourseSchedule> list = scheduleMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (CourseSchedule cs : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cs.getId());
            map.put("weekDay", cs.getWeekDay());
            map.put("sectionStart", cs.getSectionStart());
            map.put("sectionEnd", cs.getSectionEnd());
            map.put("location", cs.getLocation());
            map.put("currentCount", cs.getCurrentCount());
            map.put("maxCapacity", cs.getMaxCapacity());

            // 关联查询名称
            map.put("courseName", courseMapper.selectById(cs.getCourseId()).getName());
            map.put("teacherName", teacherMapper.selectById(cs.getTeacherId()).getName());

            // 原始ID也返回，方便编辑
            map.put("courseId", cs.getCourseId());
            map.put("teacherId", cs.getTeacherId());

            result.add(map);
        }
        return result;
    }

    // 2. 新增或修改排课
    @PostMapping("/save")
    public boolean save(@RequestBody CourseSchedule schedule) {
        if (schedule.getId() == null) {
            return scheduleMapper.insert(schedule) > 0;
        } else {
            return scheduleMapper.updateById(schedule) > 0;
        }
    }

    // 3. 删除排课
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return scheduleMapper.deleteById(id) > 0;
    }
}