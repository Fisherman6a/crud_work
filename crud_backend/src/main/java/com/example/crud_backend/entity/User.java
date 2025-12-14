package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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

    private String relatedId;
    
    @TableField(exist = false)
    private String captchaKey; // 验证码的 Key

    @TableField(exist = false)
    private String captchaCode; // 用户输入的验证码
}