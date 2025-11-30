package com.example.crud_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crud_backend.entity.Student;
import com.example.crud_backend.mapper.StudentMapper;
import com.example.crud_backend.service.IStudentService;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements IStudentService {
    // 核心业务逻辑实现（MyBatis-Plus 默认已实现绝大多数逻辑）
}