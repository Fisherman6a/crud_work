package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_student")
public class Student implements Serializable {
    // 学号作为主键，类型为 INPUT (手动输入)
    @TableId(value = "student_number", type = IdType.INPUT)
    private String studentNumber;

    private String name;
    private String gender;
    private Integer age;
    private String className;
    private String phone; // 手机号，用于发送短信通知
    private LocalDateTime createTime;
}