package com.example.crud_backend.dto.request;

import lombok.Data;

/**
 * 搜索请求DTO
 */
@Data
public class SearchRequest {
    private String keyword;        // 搜索关键词
    private String resourceType;   // 资源类型（可选）
    private Long courseId;         // 课程ID（可选）
    private Integer pageNum = 1;   // 页码
    private Integer pageSize = 50; // 每页大小
}
