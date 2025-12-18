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
    public Object save(@RequestBody java.util.Map<String, Object> request) {
        // ID is now required for manual input
        if (!request.containsKey("id") || request.get("id") == null) {
            return createErrorResponse("教师ID不能为空");
        }

        Long teacherId = Long.valueOf(request.get("id").toString());

        // Check if ID already exists
        Teacher existing = teacherMapper.selectById(teacherId);

        // 从前端获取 originalId（编辑模式下，originalId 就是当前记录的ID）
        Long originalId = null;
        if (request.containsKey("originalId") && request.get("originalId") != null) {
            originalId = Long.valueOf(request.get("originalId").toString());
        }

        // 判断是否为新增模式：originalId 为 null 表示新增
        boolean isAddMode = (originalId == null);

        if (isAddMode && existing != null) {
            // 新增模式下，如果ID已存在，拒绝
            return createErrorResponse("教师ID " + teacherId + " 已存在，请使用其他ID");
        }

        // 构建 Teacher 对象
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        teacher.setName((String) request.get("name"));
        teacher.setTitle((String) request.get("title"));
        teacher.setPhone((String) request.get("phone"));

        if (existing == null) {
            // New teacher
            return teacherMapper.insert(teacher) > 0;
        } else {
            // Update existing teacher
            return teacherMapper.updateById(teacher) > 0;
        }
    }

    private Object createErrorResponse(String message) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    // 4. 删除
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return teacherMapper.deleteById(id) > 0;
    }
}