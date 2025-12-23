package com.example.crud_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 资源响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {
    private Long id;
    private Long courseId;
    private String courseName;           // 课程名称
    private String teacherName;          // 教师名称
    private String resourceName;         // 资源名称
    private String resourceType;         // 资源类型
    private String resourceUrl;          // 资源URL
    private LocalDateTime createTime;    // 创建时间
    private Map<String, String> highlights; // 高亮字段（搜索结果使用）
    private Double score;                   // 搜索得分（搜索结果使用）
}
