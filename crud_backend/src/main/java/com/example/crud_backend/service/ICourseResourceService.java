package com.example.crud_backend.service;

import com.example.crud_backend.dto.request.SearchRequest;
import com.example.crud_backend.dto.response.ResourceResponse;
import com.example.crud_backend.dto.response.SearchResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 课程资源服务接口
 */
public interface ICourseResourceService {

    /**
     * 根据课程ID获取资源列表
     */
    List<ResourceResponse> getResourcesByCourseId(Long courseId);

    /**
     * 上传资源文件
     */
    Map<String, Object> uploadResource(Long courseId, MultipartFile file);

    /**
     * 删除资源
     */
    boolean deleteResource(Long resourceId);

    /**
     * 获取资源预览URL
     */
    Map<String, Object> getResourcePreviewUrl(Long resourceId);

    /**
     * 搜索资源（Elasticsearch全文搜索）
     */
    SearchResponse searchResources(SearchRequest request);

    /**
     * 将资源同步到Elasticsearch
     */
    boolean syncResourceToElasticsearch(Long resourceId);

    /**
     * 将所有资源同步到Elasticsearch
     */
    Map<String, Object> syncAllResourcesToElasticsearch();
}
