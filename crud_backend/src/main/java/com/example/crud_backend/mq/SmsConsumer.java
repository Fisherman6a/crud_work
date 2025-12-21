package com.example.crud_backend.mq;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 短信消费者 - 从RabbitMQ队列中消费短信消息并调用第三方短信API发送
 *
 * 使用说明：
 * 1. 如需集成阿里云短信服务，请添加依赖：
 *    <dependency>
 *        <groupId>com.aliyun</groupId>
 *        <artifactId>dysmsapi20170525</artifactId>
 *        <version>2.0.24</version>
 *    </dependency>
 *
 * 2. 在application.yml中配置：
 *    aliyun:
 *      sms:
 *        access-key-id: your_access_key_id
 *        access-key-secret: your_access_key_secret
 *        sign-name: your_sign_name
 *        template-code: your_template_code
 *
 * 3. 取消下方代码注释即可启用真实短信发送功能
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

    /**
     * 调用阿里云短信API发送短信
     *
     * 集成步骤（取消注释后使用）：
     *
     * import com.aliyun.dysmsapi20170525.Client;
     * import com.aliyun.dysmsapi20170525.models.*;
     * import com.aliyun.teaopenapi.models.Config;
     *
     * private void sendAliYunSms(String phone, String message) throws Exception {
     *     Config config = new Config()
     *         .setAccessKeyId(accessKeyId)
     *         .setAccessKeySecret(accessKeySecret)
     *         .setEndpoint("dysmsapi.aliyuncs.com");
     *
     *     Client client = new Client(config);
     *
     *     SendSmsRequest request = new SendSmsRequest()
     *         .setPhoneNumbers(phone)
     *         .setSignName(signName)
     *         .setTemplateCode(templateCode)
     *         .setTemplateParam("{\"message\":\"" + message + "\"}");
     *
     *     SendSmsResponse response = client.sendSms(request);
     *
     *     if (!"OK".equals(response.getBody().getCode())) {
     *         throw new RuntimeException("短信发送失败: " + response.getBody().getMessage());
     *     }
     * }
     */
    private void sendAliYunSms(String phone, String message) throws Exception {
        // TODO: 取消上方注释的代码并添加阿里云SDK依赖后即可启用
        System.out.println(">>> 阿里云短信API集成代码待启用（请参考注释中的集成步骤）");
    }
}
