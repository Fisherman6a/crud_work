package com.example.crud_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/check")
    public String checkEnvironment() {
        StringBuilder result = new StringBuilder();
        result.append("<h1>环境连接测试报告</h1>");

        // --- 1. 测试 Redis 连接 ---
        try {
            // 写入一个数据
            redisTemplate.opsForValue().set("test_hello", "Redis is working!");
            // 读取这个数据
            String value = redisTemplate.opsForValue().get("test_hello");
            result.append("<p style='color:green'>✅ Redis 连接成功！读取到值: " + value + "</p>");
        } catch (Exception e) {
            result.append("<p style='color:red'>❌ Redis 连接失败: " + e.getMessage() + "</p>");
        }

        // --- 2. 测试 MySQL 连接 ---
        try {
            // 执行一个最简单的查询 select 1
            List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT 1");
            result.append("<p style='color:green'>✅ MySQL 连接成功！查询结果: " + list + "</p>");
        } catch (Exception e) {
            result.append("<p style='color:red'>❌ MySQL 连接失败: " + e.getMessage() + "</p>");
        }

        // --- 3. Nacos 状态 ---
        result.append(
                "<p style='color:blue'>ℹ️ Nacos 状态: 请去 localhost:8848 网页查看服务列表，如果看到 suep-student-service 就是成功。</p>");

        return result.toString();
    }
}