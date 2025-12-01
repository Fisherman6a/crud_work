package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crud_backend.entity.User;
import com.example.crud_backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginUser) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询数据库
        User dbUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginUser.getUsername())
                .eq(User::getPassword, loginUser.getPassword()));

        if (dbUser != null) {
            // 2. 生成 Token
            String token = UUID.randomUUID().toString();
            // 3. 存入 Redis (Key: token, Value: role, 过期时间 30分钟)
            redisTemplate.opsForValue().set("token:" + token, dbUser.getRole());

            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("token", token);
            result.put("role", dbUser.getRole()); // 返回给前端用于控制界面
            result.put("username", dbUser.getUsername());
        } else {
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");
        }
        return result;
    }
}