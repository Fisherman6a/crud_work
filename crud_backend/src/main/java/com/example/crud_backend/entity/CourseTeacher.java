package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_course_teacher")
public class CourseTeacher {
    @TableId(type = IdType.AUTO)  // 使用数据库自增ID
    private Long id;
    private Long courseId;
    private Long teacherId;
}
