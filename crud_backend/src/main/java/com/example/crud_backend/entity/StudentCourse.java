package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_student_course")
public class StudentCourse {
    @TableId(type = IdType.ASSIGN_ID)  // 使用雪花算法自动生成ID
    private Long id;
    private String studentId;
    private Long scheduleId;
    private LocalDateTime createTime;
    private BigDecimal score;
}