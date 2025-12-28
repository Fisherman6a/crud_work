package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crud_backend.entity.Student;
import com.example.crud_backend.service.IStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private IStudentService studentService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 1. 新增学生 (包含主键冲突检测)
    @PostMapping
    public Map<String, Object> add(@RequestBody Student student) {
        Map<String, Object> res = new HashMap<>();
        try {
            // MybatisPlus 的 save 方法如果主键重复会抛出异常
            boolean success = studentService.save(student);
            res.put("code", success ? 200 : 500);
            res.put("msg", success ? "新增成功" : "新增失败");
        } catch (DuplicateKeyException e) {
            // 检测到主键冲突
            res.put("code", 409);
            res.put("msg", "错误：学号 [" + student.getStudentNumber() + "] 已存在！");
        } catch (Exception e) {
            res.put("code", 500);
            res.put("msg", "系统异常：" + e.getMessage());
        }
        return res;
    }

    // 2. 删除学生
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        boolean success = studentService.removeById(id);
        return success ? "删除成功" : "删除失败";
    }

    // 3. 修改学生
    @PutMapping
    public Map<String, Object> update(@RequestBody Student student) {
        // updateById 会根据 studentNumber 去更新其他字段
        boolean success = studentService.updateById(student);
        Map<String, Object> res = new HashMap<>();
        res.put("code", success ? 200 : 500);
        res.put("msg", success ? "修改成功" : "修改失败(学号不存在)");
        return res;
    }

    // 4. 根据ID查询
    @GetMapping("/{id}")
    public Student getById(@PathVariable Long id) {
        return studentService.getById(id);
    }

    // 5. 分页查询 (支持按姓名模糊搜索)
    // 请求示例: /student/page?pageNum=1&pageSize=10&name=张
    @GetMapping("/page")
    public Object page(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name) {

        Page<Student> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();

        // 如果传递了 name 参数，则进行模糊查询
        if (StringUtils.hasText(name)) {
            wrapper.like(Student::getName, name);
        }
        // 按创建时间倒序
        wrapper.orderByDesc(Student::getCreateTime);

        return studentService.page(page, wrapper);
    }
}