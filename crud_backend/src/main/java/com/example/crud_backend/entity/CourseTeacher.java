package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_course_teacher")
public class CourseTeacher {
    @TableId(type = IdType.ASSIGN_ID)  // 使用雪花算法自动生成ID
    private Long id;
    private Long courseId;
    private Long teacherId;
}
