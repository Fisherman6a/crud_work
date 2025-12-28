package com.example.crud_backend.mq;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 短信消费者 - 从RabbitMQ队列中消费短信消息并调用第三方短信API发送
 */
@Component
@RabbitListener(queues = "sms.queue")
public class SmsConsumer {

    @Value("${aliyun.sms.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.sms.sign-name:教务系统}")
    private String signName;

    @Value("${aliyun.sms.template-code:}")
    private String templateCode;

    @RabbitHandler
    public void process(Map<String, String> smsMap) {
        String phone = smsMap.get("phone");
        String message = smsMap.get("msg");
        String type = smsMap.get("type");

        System.out.println("=== 短信发送任务 ===");
        System.out.println("接收人: " + phone);
        System.out.println("消息类型: " + type);
        System.out.println("内容: " + message);

        // 模拟短信发送（开发环境）
        if (accessKeyId == null || accessKeyId.isEmpty()) {
            System.out.println(">>> 模拟发送成功 (未配置阿里云短信服务)");
            return;
        }

        // 真实短信发送（生产环境）
        try {
            sendAliYunSms(phone, message);
            System.out.println(">>> 短信发送成功");
        } catch (Exception e) {
            System.err.println(">>> 短信发送失败: " + e.getMessage());
        }
    }

    private void sendAliYunSms(String phone, String message) throws Exception {
        System.out.println(">>> 阿里云短信API还未秦勇");
    }
}
