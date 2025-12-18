package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.crud_backend.entity.CourseResource;
import com.example.crud_backend.mapper.CourseResourceMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/resource")
@CrossOrigin(origins = "*") // 允许前端跨域调用
public class CourseResourceController {

    @Autowired
    private CourseResourceMapper resourceMapper;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${minio.bucket-name:course-bucket}")
    private String bucketName;

    @GetMapping("/list/{courseId}")
    public List<CourseResource> listByCourse(@PathVariable Long courseId) {
        QueryWrapper<CourseResource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        return resourceMapper.selectList(queryWrapper);
    }

    @PostMapping("/save")
    public boolean save(@RequestBody CourseResource resource) {
        resource.setCreateTime(LocalDateTime.now());
        return resourceMapper.insert(resource) > 0;
    }

    // 上传课程资源（集成 MinIO）
    @PostMapping("/upload")
    public Map<String, Object> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "resourceName", required = false) String resourceName) throws Exception {

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "-" + originalFilename;

            // 2. 上传文件到 MinIO
            if (minioClient != null) {
                try (InputStream is = file.getInputStream()) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(fileName)
                                    .stream(is, file.getSize(), -1)
                                    .contentType(file.getContentType())
                                    .build());
                }
            }

            // 3. 保存记录到数据库
            CourseResource resource = new CourseResource();
            resource.setCourseId(courseId);
            resource.setResourceName(resourceName != null ? resourceName : originalFilename);
            resource.setResourceType(getFileExtension(originalFilename));
            resource.setResourceUrl(fileName);
            resource.setCreateTime(LocalDateTime.now());

            resourceMapper.insert(resource);

            result.put("success", true);
            result.put("resourceId", resource.getId());
            result.put("fileName", fileName);
            result.put("message", "上传成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            throw e;
        }

        return result;
    }

    // 获取资源预览URL
    @GetMapping("/preview/{resourceId}")
    public Map<String, String> getPreviewUrl(@PathVariable Long resourceId) throws Exception {
        CourseResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "资源不存在");
            return error;
        }

        String url = "";
        if (minioClient != null) {
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(resource.getResourceUrl())
                            .expiry(24 * 60 * 60) // 24小时有效期
                            .build());
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("fileName", resource.getResourceName());
        result.put("fileType", resource.getResourceType());
        return result;
    }

    // 删除资源
    @DeleteMapping("/{resourceId}")
    public Map<String, Object> deleteResource(@PathVariable Long resourceId) {
        Map<String, Object> result = new HashMap<>();
        int rows = resourceMapper.deleteById(resourceId);
        result.put("success", rows > 0);
        return result;
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}