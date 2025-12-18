package com.example.crud_backend.controller;

import com.example.crud_backend.entity.Notification;
import com.example.crud_backend.service.INotificationService;
import com.example.crud_backend.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notification")
@CrossOrigin
public class NotificationController {

    @Autowired
    private INotificationService notificationService;

    @GetMapping("/list/{userId}")
    public Map<String, Object> getNotificationList(@PathVariable String userId, @RequestParam(required = false) Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Notification> notifications = notificationService.getNotificationsByUserId(userId, limit);
            result.put("code", 200);
            result.put("data", notifications);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/unread-count/{userId}")
    public Map<String, Object> getUnreadCount(@PathVariable String userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer count = notificationService.getUnreadCount(userId);
            result.put("code", 200);
            result.put("data", count);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/read/{id}")
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = notificationService.markAsRead(id);
            result.put("code", success ? 200 : 400);
            result.put("message", success ? "标记成功" : "标记失败");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteNotification(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = notificationService.deleteNotification(id);
            result.put("code", success ? 200 : 400);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/send")
    public Map<String, Object> sendNotification(@RequestBody Notification notification) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = notificationService.createNotification(notification);
            if (success) {
                // 通过WebSocket推送
                WebSocketServer.sendInfo(notification.getUserId(), notification.getTitle() + ": " + notification.getContent());
            }
            result.put("code", success ? 200 : 400);
            result.put("message", success ? "发送成功" : "发送失败");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
