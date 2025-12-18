package com.example.crud_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crud_backend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(*) FROM t_notification WHERE user_id = #{userId} AND is_read = false")
    Integer getUnreadCount(String userId);
}
