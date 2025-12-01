package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_user")
public class User {
    @TableId
    private String username;
    private String password;
    private String role; // "ADMIN" 或 "USER"
}