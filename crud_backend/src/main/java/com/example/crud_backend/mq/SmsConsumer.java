package com.example.crud_backend.mq;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RabbitListener(queues = "sms.queue")
public class SmsConsumer {

    @RabbitHandler
    public void process(Map<String, String> smsMap) {
        System.out.println("--- 模拟调用短信接口 ---");
        System.out.println("发送给: " + smsMap.get("phone"));
        System.out.println("内容: " + smsMap.get("msg"));
        // 在这里调用第三方短信API (如阿里云、腾讯云)
    }
}