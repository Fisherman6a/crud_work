package com.example.crud_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Nacos 配置中心演示
 * 用于展示从 Nacos 读取配置并支持动态刷新
 */
@RestController
@RequestMapping("/config")
@RefreshScope  // 关键注解：支持配置动态刷新
public class NacosConfigController {

    @Value("${app.name:教务管理系统}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:基于 Spring Boot 3 + Vue 3 的教务管理系统}")
    private String appDescription;

    @Value("${business.course-selection.max-courses-per-student:10}")
    private Integer maxCourses;

    @Value("${business.course-selection.allow-duplicate:false}")
    private Boolean allowDuplicate;

    @Value("${business.course-selection.check-time-conflict:true}")
    private Boolean checkTimeConflict;

    @Value("${system.announcement:欢迎使用教务管理系统！}")
    private String announcement;

    @Value("${system.maintenance-mode:false}")
    private Boolean maintenanceMode;

    /**
     * 获取从 Nacos 读取的配置信息
     * 访问: http://localhost:8080/config/info
     */
    @GetMapping("/info")
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> result = new HashMap<>();

        // 应用信息
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("name", appName);
        appInfo.put("version", appVersion);
        appInfo.put("description", appDescription);
        result.put("application", appInfo);

        // 业务配置
        Map<String, Object> businessConfig = new HashMap<>();
        businessConfig.put("maxCoursesPerStudent", maxCourses);
        businessConfig.put("allowDuplicate", allowDuplicate);
        businessConfig.put("checkTimeConflict", checkTimeConflict);
        result.put("businessConfig", businessConfig);

        // 系统配置
        Map<String, Object> systemConfig = new HashMap<>();
        systemConfig.put("announcement", announcement);
        systemConfig.put("maintenanceMode", maintenanceMode);
        result.put("systemConfig", systemConfig);

        // 说明信息
        result.put("note", "✅ 这些配置来自 Nacos 配置中心");
        result.put("feature", "✨ 在 Nacos 控制台修改配置后，点击发布，无需重启应用即可生效！");
        result.put("nacosUrl", "http://localhost:8848/nacos");

        return result;
    }

    /**
     * 获取系统公告（演示单个配置项）
     */
    @GetMapping("/announcement")
    public Map<String, String> getAnnouncement() {
        Map<String, String> result = new HashMap<>();
        result.put("announcement", announcement);
        result.put("source", "Nacos Config Center");
        return result;
    }

    /**
     * 获取选课规则配置
     */
    @GetMapping("/course-rules")
    public Map<String, Object> getCourseRules() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("maxCourses", maxCourses);
        rules.put("allowDuplicate", allowDuplicate);
        rules.put("checkTimeConflict", checkTimeConflict);
        rules.put("description", "这些规则可以在 Nacos 中动态调整");
        return rules;
    }
}
