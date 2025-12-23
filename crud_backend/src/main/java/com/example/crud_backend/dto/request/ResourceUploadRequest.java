package com.example.crud_backend.dto.request;

import lombok.Data;

/**
 * 资源上传请求DTO
 */
@Data
public class ResourceUploadRequest {
    private Long courseId;         // 课程ID
    private String resourceName;   // 资源名称
    private String resourceType;   // 资源类型
}
