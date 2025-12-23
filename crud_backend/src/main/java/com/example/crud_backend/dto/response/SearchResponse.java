package com.example.crud_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private Long total;                         // 总数
    private List<ResourceResponse> data;        // 搜索结果列表
}
