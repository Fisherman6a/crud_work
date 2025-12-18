package com.example.crud_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crud_backend.entity.Notification;
import com.example.crud_backend.mapper.NotificationMapper;
import com.example.crud_backend.service.INotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    @Override
    public List<Notification> getNotificationsByUserId(String userId, Integer limit) {
        QueryWrapper<Notification> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        qw.orderByDesc("create_time");
        if (limit != null && limit > 0) {
            qw.last("LIMIT " + limit);
        }
        return this.list(qw);
    }

    @Override
    public Integer getUnreadCount(String userId) {
        return baseMapper.getUnreadCount(userId);
    }

    @Override
    public boolean markAsRead(Long id) {
        Notification notification = this.getById(id);
        if (notification != null) {
            notification.setIsRead(true);
            return this.updateById(notification);
        }
        return false;
    }

    @Override
    public boolean deleteNotification(Long id) {
        return this.removeById(id);
    }

    @Override
    public boolean createNotification(Notification notification) {
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }
        return this.save(notification);
    }
}
