package com.example.crud_backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private Long total;        // 总记录数
    private List<T> data;      // 数据列表
    private Integer pageNum;   // 当前页码
    private Integer pageSize;  // 每页大小
}
