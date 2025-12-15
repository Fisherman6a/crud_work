package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.crud_backend.entity.CourseResource;
import com.example.crud_backend.mapper.CourseResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/resource")
@CrossOrigin(origins = "*") // 允许前端跨域调用
public class CourseResourceController {

    @Autowired
    private CourseResourceMapper resourceMapper;

    @GetMapping("/list/{courseId}")
    public List<CourseResource> listByCourse(@PathVariable Long courseId) {
        QueryWrapper<CourseResource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        return resourceMapper.selectList(queryWrapper);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody CourseResource resource) {
        resource.setCreateTime(LocalDateTime.now());
        return resourceMapper.insert(resource) > 0;
    }
}