package com.example.crud_backend.controller;

import com.example.crud_backend.websocket.WebSocketServer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/student-course")
@CrossOrigin(origins = "*")
public class StudentCourseController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 模拟选课
    @PostMapping("/select")
    public String selectCourse(@RequestBody Map<String, Object> params) {
        String studentId = (String) params.get("studentId");
        String courseName = (String) params.get("courseName");
        String phoneNumber = (String) params.get("phoneNumber"); // 模拟从库里查出来的

        // ... 执行选课数据库逻辑 (省略) ...

        // 1. 发送站内信 (WebSocket)
        WebSocketServer.sendInfo(studentId, "选课成功通知：您已成功选择 " + courseName);

        // 2. 发送短信 (RabbitMQ 异步解耦)
        Map<String, String> smsMap = new HashMap<>();
        smsMap.put("phone", phoneNumber);
        smsMap.put("msg", "您好，您已成功选修课程：" + courseName);
        rabbitTemplate.convertAndSend("sms.queue", smsMap);

        return "选课成功";
    }

    // 获取课程日历
    @GetMapping("/calendar")
    public Object getCourseCalendar(@RequestParam String studentId) {
        // ... 查询数据库返回该学生的课程时间表 ...
        return "返回课程时间JSON数据";
    }
}