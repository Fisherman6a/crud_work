package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_student") // 对应数据库表名
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private String studentNumber; // 学号
    private String name; // 姓名
    private String gender; // 性别
    private Integer age; // 年龄
    private String className; // 班级

    // 如果你希望自动填充时间，可以在MyBatisPlusConfig中配置，这里简单起见主要由数据库处理
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}