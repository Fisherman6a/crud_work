package com.example.crud_backend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping
    public Map<String, String> getCaptcha() {
        // 1. 生成验证码图片 (宽120，高40，4个字符，干扰线20条)
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);

        // 2. 获取验证码里的文字 (比如 "AB12")
        String code = lineCaptcha.getCode();

        // 3. 生成一个唯一标识 key，用于前端后续提交验证
        String key = UUID.randomUUID().toString();

        // 4. 存入 Redis，设置 2 分钟过期 (Key: captcha:xxxx-xxxx, Value: AB12)
        redisTemplate.opsForValue().set("captcha:" + key, code, 2, TimeUnit.MINUTES);

        // 5. 返回 Base64 图片数据和 key 给前端
        Map<String, String> result = new HashMap<>();
        result.put("key", key);
        result.put("image", lineCaptcha.getImageBase64Data()); // 返回 base64 图片字符串

        return result;
    }
}