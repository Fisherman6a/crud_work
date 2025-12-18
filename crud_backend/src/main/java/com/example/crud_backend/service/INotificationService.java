package com.example.crud_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crud_backend.entity.Notification;

import java.util.List;

public interface INotificationService extends IService<Notification> {

    List<Notification> getNotificationsByUserId(String userId, Integer limit);

    Integer getUnreadCount(String userId);

    boolean markAsRead(Long id);

    boolean deleteNotification(Long id);

    boolean createNotification(Notification notification);
}
