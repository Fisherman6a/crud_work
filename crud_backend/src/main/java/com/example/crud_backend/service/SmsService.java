package com.example.crud_backend.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信服务 - RabbitMQ消息生产者
 * 将短信消息发送到消息队列，由消费者异步处理
 */
@Service
public class SmsService {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送选课成功短信通知
     * @param phone 手机号
     * @param studentName 学生姓名
     * @param courseName 课程名称
     */
    public void sendCourseSelectionSms(String phone, String studentName, String courseName) {
        if (rabbitTemplate == null) {
            System.out.println("RabbitMQ未配置，跳过短信发送");
            return;
        }

        Map<String, String> smsMap = new HashMap<>();
        smsMap.put("phone", phone);
        smsMap.put("msg", String.format("【教务系统】%s同学，您已成功选修《%s》课程。", studentName, courseName));
        smsMap.put("type", "COURSE_SELECTION");

        rabbitTemplate.convertAndSend("sms.queue", smsMap);
        System.out.println("选课短信已发送到消息队列: " + phone);
    }

    /**
     * 发送退课成功短信通知
     * @param phone 手机号
     * @param studentName 学生姓名
     * @param courseName 课程名称
     */
    public void sendCourseDropSms(String phone, String studentName, String courseName) {
        if (rabbitTemplate == null) {
            System.out.println("RabbitMQ未配置，跳过短信发送");
            return;
        }

        Map<String, String> smsMap = new HashMap<>();
        smsMap.put("phone", phone);
        smsMap.put("msg", String.format("【教务系统】%s同学，您已成功退选《%s》课程。", studentName, courseName));
        smsMap.put("type", "COURSE_DROP");

        rabbitTemplate.convertAndSend("sms.queue", smsMap);
        System.out.println("退课短信已发送到消息队列: " + phone);
    }

    /**
     * 发送课程变更通知短信
     * @param phone 手机号
     * @param studentName 学生姓名
     * @param courseName 课程名称
     * @param changeInfo 变更信息
     */
    public void sendCourseChangeSms(String phone, String studentName, String courseName, String changeInfo) {
        if (rabbitTemplate == null) {
            System.out.println("RabbitMQ未配置，跳过短信发送");
            return;
        }

        Map<String, String> smsMap = new HashMap<>();
        smsMap.put("phone", phone);
        smsMap.put("msg", String.format("【教务系统】%s同学，您选修的《%s》课程有变更：%s", studentName, courseName, changeInfo));
        smsMap.put("type", "COURSE_CHANGE");

        rabbitTemplate.convertAndSend("sms.queue", smsMap);
        System.out.println("课程变更短信已发送到消息队列: " + phone);
    }

    /**
     * 发送课程提醒短信
     * @param phone 手机号
     * @param studentName 学生姓名
     * @param courseName 课程名称
     * @param time 上课时间
     * @param location 上课地点
     */
    public void sendCourseReminderSms(String phone, String studentName, String courseName, String time, String location) {
        if (rabbitTemplate == null) {
            System.out.println("RabbitMQ未配置，跳过短信发送");
            return;
        }

        Map<String, String> smsMap = new HashMap<>();
        smsMap.put("phone", phone);
        smsMap.put("msg", String.format("【教务系统】%s同学，您有课程《%s》将于%s在%s上课，请准时出席。",
            studentName, courseName, time, location));
        smsMap.put("type", "COURSE_REMINDER");

        rabbitTemplate.convertAndSend("sms.queue", smsMap);
        System.out.println("课程提醒短信已发送到消息队列: " + phone);
    }
}
