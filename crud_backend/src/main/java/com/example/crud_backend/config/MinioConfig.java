package com.example.crud_backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 * 自动创建bucket并设置为公开读取
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 使用CommandLineRunner确保在MinioClient bean创建后执行
     * 自动创建bucket(如果不存在)
     */
    @Bean
    public CommandLineRunner initializeBucket(@Autowired MinioClient minioClient) {
        return args -> {
            try {
                // 检查bucket是否存在
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

                if (!exists) {
                    // 创建bucket
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(bucketName)
                                    .build()
                    );
                    System.out.println("✅ MinIO bucket 创建成功: " + bucketName);

                    // 设置bucket为公开读取(方便预览)
                    String policy = "{\n" +
                            "  \"Version\": \"2012-10-17\",\n" +
                            "  \"Statement\": [\n" +
                            "    {\n" +
                            "      \"Effect\": \"Allow\",\n" +
                            "      \"Principal\": {\"AWS\": \"*\"},\n" +
                            "      \"Action\": [\"s3:GetObject\"],\n" +
                            "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}";

                    minioClient.setBucketPolicy(
                            SetBucketPolicyArgs.builder()
                                    .bucket(bucketName)
                                    .config(policy)
                                    .build()
                    );
                    System.out.println("✅ MinIO bucket 权限设置成功(公开读取)");
                } else {
                    System.out.println("✅ MinIO bucket 已存在: " + bucketName);
                }
            } catch (Exception e) {
                System.err.println("❌ MinIO初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}