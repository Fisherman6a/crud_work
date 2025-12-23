package com.example.crud_backend.controller;

import com.example.crud_backend.dto.common.Result;
import com.example.crud_backend.dto.response.ResourceResponse;
import com.example.crud_backend.service.ICourseResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 课程资源控制器
 * 职责：接收HTTP请求，调用Service层，返回响应
 */
@RestController
@RequestMapping("/resource")
public class CourseResourceController {

    @Autowired
    private ICourseResourceService courseResourceService;

    /**
     * 根据课程ID获取资源列表
     */
    @GetMapping("/list/{courseId}")
    public List<ResourceResponse> listByCourse(@PathVariable Long courseId) {
        return courseResourceService.getResourcesByCourseId(courseId);
    }

    /**
     * 上传课程资源（集成 MinIO + Elasticsearch）
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId) {
        return courseResourceService.uploadResource(courseId, file);
    }

    /**
     * 获取资源预览URL
     */
    @GetMapping("/preview/{resourceId}")
    public Map<String, Object> getPreviewUrl(@PathVariable Long resourceId) {
        return courseResourceService.getResourcePreviewUrl(resourceId);
    }

    /**
     * 删除资源
     */
    @DeleteMapping("/{resourceId}")
    public Map<String, Object> deleteResource(@PathVariable Long resourceId) {
        boolean success = courseResourceService.deleteResource(resourceId);
        return Map.of("success", success);
    }

    /**
     * 同步单个资源到 Elasticsearch
     */
    @PostMapping("/sync-to-es/{resourceId}")
    public Result<Boolean> syncToElasticsearch(@PathVariable Long resourceId) {
        boolean success = courseResourceService.syncResourceToElasticsearch(resourceId);
        return Result.success(success);
    }

    /**
     * 同步所有资源到 Elasticsearch
     * 用于首次初始化索引
     */
    @PostMapping("/sync-all-to-es")
    public Map<String, Object> syncAllToElasticsearch() {
        return courseResourceService.syncAllResourcesToElasticsearch();
    }
}
