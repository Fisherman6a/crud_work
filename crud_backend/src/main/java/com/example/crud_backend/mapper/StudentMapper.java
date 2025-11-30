package com.example.crud_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crud_backend.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    // MyBatis-Plus 已内置基础 CRUD，无需手写 SQL
}