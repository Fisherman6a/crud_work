package com.example.crud_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crud_backend.entity.StudentCourse;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentCourseMapper extends BaseMapper<StudentCourse> {
}
