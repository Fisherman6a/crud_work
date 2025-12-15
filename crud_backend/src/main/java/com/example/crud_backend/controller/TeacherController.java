package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crud_backend.entity.Teacher;
import com.example.crud_backend.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teacher")
@CrossOrigin(origins = "*") // 允许前端跨域调用
public class TeacherController {

    @Autowired
    private TeacherMapper teacherMapper;

    // 1. 获取所有教师（用于下拉框选择）
    @GetMapping("/list")
    public List<Teacher> list() {
        return teacherMapper.selectList(null);
    }

    // 2. 分页查询（用于管理页面表格）
    @GetMapping("/page")
    public Page<Teacher> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String search) {
        Page<Teacher> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Teacher> queryWrapper = new QueryWrapper<>();
        if (!"".equals(search)) {
            queryWrapper.like("name", search);
        }
        return teacherMapper.selectPage(page, queryWrapper);
    }

    // 3. 新增或更新
    @PostMapping("/save")
    public boolean save(@RequestBody Teacher teacher) {
        if (teacher.getId() == null) {
            return teacherMapper.insert(teacher) > 0;
        } else {
            return teacherMapper.updateById(teacher) > 0;
        }
    }

    // 4. 删除
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return teacherMapper.deleteById(id) > 0;
    }
}