package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_course_resource")
public class CourseResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId; // 关联 t_course
    private String resourceName;
    private String resourceType;
    private String resourceUrl;
    private LocalDateTime createTime;
}