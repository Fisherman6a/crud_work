package com.example.crud_backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class SystemConfigController {

    // 模拟的选课时间配置（实际项目中应该存储在数据库或配置文件中）
    private static long mockCurrentTime = System.currentTimeMillis();
    private static long openStart = 1725120000000L; // 2025-09-01 00:00:00
    private static long openEnd = 1725984000000L;   // 2025-09-10 00:00:00

    // 获取系统配置（包括当前时间和选课时间窗口）
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("currentTime", mockCurrentTime);
        config.put("openStart", openStart);
        config.put("openEnd", openEnd);
        return config;
    }

    // 更新系统时间（仅用于测试）
    @PostMapping("/time")
    public Map<String, Object> updateTime(@RequestBody Map<String, Object> body) {
        if (body.containsKey("currentTime")) {
            Object timeObj = body.get("currentTime");
            if (timeObj instanceof Number) {
                mockCurrentTime = ((Number) timeObj).longValue();
            } else if (timeObj instanceof String) {
                mockCurrentTime = Long.parseLong((String) timeObj);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("currentTime", mockCurrentTime);
        result.put("openStart", openStart);
        result.put("openEnd", openEnd);
        return result;
    }

    // 更新选课时间窗口（管理员功能）
    @PostMapping("/config")
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> body) {
        if (body.containsKey("openStart")) {
            openStart = ((Number) body.get("openStart")).longValue();
        }
        if (body.containsKey("openEnd")) {
            openEnd = ((Number) body.get("openEnd")).longValue();
        }

        return getConfig();
    }
}
