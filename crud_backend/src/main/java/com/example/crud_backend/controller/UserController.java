package com.example.crud_backend.controller;

import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crud_backend.entity.User;
import com.example.crud_backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

        // --- 1. 新增：验证码校验逻辑 ---
        String key = loginUser.getCaptchaKey();
        String code = loginUser.getCaptchaCode();

        if (!StringUtils.hasText(key) || !StringUtils.hasText(code)) {
            result.put("code", 400);
            result.put("msg", "请输入验证码");
            return result;
        }

        // 去 Redis 查真正的答案
        String realCode = redisTemplate.opsForValue().get("captcha:" + key);

        if (realCode == null) {
            result.put("code", 400);
            result.put("msg", "验证码已过期，请刷新");
            return result;
        }

        // 忽略大小写比较
        if (!realCode.equalsIgnoreCase(code)) {
            result.put("code", 400);
            result.put("msg", "验证码错误");
            return result;
        }

        // 验证通过后，为了安全，建议立刻删除 Redis 里的这个 key (防止重复使用)
        redisTemplate.delete("captcha:" + key);

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
            result.put("relatedId", dbUser.getRelatedId());
        } else {
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");
        }
        return result;
    }
}