package com.example.crud_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.example.crud_backend.entity.Teacher;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
}
