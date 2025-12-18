package com.example.crud_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crud_backend.entity.Course;
import com.example.crud_backend.mapper.CourseMapper;
import com.example.crud_backend.service.ICourseService;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {
    // MyBatis-Plus 已经提供了基础的 CRUD 方法
    // 如需自定义业务逻辑，可以在这里添加
}
