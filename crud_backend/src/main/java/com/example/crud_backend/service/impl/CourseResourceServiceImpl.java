package com.example.crud_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.crud_backend.document.CourseResourceDocument;
import com.example.crud_backend.dto.request.SearchRequest;
import com.example.crud_backend.dto.response.ResourceResponse;
import com.example.crud_backend.dto.response.SearchResponse;
import com.example.crud_backend.entity.Course;
import com.example.crud_backend.entity.CourseResource;
import com.example.crud_backend.entity.CourseSchedule;
import com.example.crud_backend.entity.Teacher;
import com.example.crud_backend.mapper.CourseMapper;
import com.example.crud_backend.mapper.CourseResourceMapper;
import com.example.crud_backend.mapper.CourseScheduleMapper;
import com.example.crud_backend.mapper.TeacherMapper;
import com.example.crud_backend.service.ICourseResourceService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程资源服务实现类
 */
@Service
public class CourseResourceServiceImpl implements ICourseResourceService {

    @Autowired
    private CourseResourceMapper resourceMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${minio.bucket-name:course-files}")
    private String bucketName;

    @Override
    public List<ResourceResponse> getResourcesByCourseId(Long courseId) {
        QueryWrapper<CourseResource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        List<CourseResource> resources = resourceMapper.selectList(queryWrapper);

        return resources.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> uploadResource(Long courseId, MultipartFile file) {
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
            resource.setResourceName(originalFilename);
            resource.setResourceType(getFileExtension(originalFilename));
            resource.setResourceUrl(fileName);
            resource.setCreateTime(LocalDateTime.now());

            resourceMapper.insert(resource);

            // 4. 同步到 Elasticsearch
            syncToElasticsearch(resource);

            result.put("success", true);
            result.put("resourceId", resource.getId());
            result.put("fileName", fileName);
            result.put("message", "上传成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public boolean deleteResource(Long resourceId) {
        // 从 Elasticsearch 删除
        if (elasticsearchOperations != null) {
            try {
                elasticsearchOperations.delete(resourceId.toString(), CourseResourceDocument.class);
                System.out.println("✅ 已从 Elasticsearch 删除资源: " + resourceId);
            } catch (Exception e) {
                System.err.println("❌ Elasticsearch 删除失败: " + e.getMessage());
            }
        }

        // 从数据库删除
        return resourceMapper.deleteById(resourceId) > 0;
    }

    @Override
    public Map<String, Object> getResourcePreviewUrl(Long resourceId) {
        CourseResource resource = resourceMapper.selectById(resourceId);
        Map<String, Object> result = new HashMap<>();

        if (resource == null) {
            result.put("error", "资源不存在");
            return result;
        }

        String url = "";
        if (minioClient != null) {
            try {
                url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(resource.getResourceUrl())
                                .expiry(24 * 60 * 60) // 24小时有效期
                                .build());
            } catch (Exception e) {
                result.put("error", "获取URL失败: " + e.getMessage());
                return result;
            }
        }

        result.put("url", url);
        result.put("fileName", resource.getResourceName());
        result.put("fileType", resource.getResourceType());
        return result;
    }

    @Override
    public SearchResponse searchResources(SearchRequest request) {
        if (elasticsearchOperations == null) {
            return new SearchResponse(0L, new ArrayList<>());
        }

        try {
            // 构建查询条件
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // 关键词匹配
            if (StringUtils.hasText(request.getKeyword())) {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                        .query(request.getKeyword())
                        .fields("resourceName^3.0", "courseName^2.0", "teacherName^1.5", "courseDescription^1.0")
                        .type(TextQueryType.BestFields)
                        .fuzziness("AUTO")
                );
                boolQueryBuilder.must(Query.of(q -> q.multiMatch(multiMatchQuery)));
            }

            // 资源类型过滤
            if (StringUtils.hasText(request.getResourceType())) {
                boolQueryBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("resourceType")
                        .value(request.getResourceType())
                )));
            }

            // 课程ID过滤
            if (request.getCourseId() != null) {
                boolQueryBuilder.filter(Query.of(q -> q.term(t -> t
                        .field("courseId")
                        .value(request.getCourseId())
                )));
            }

            // 高亮配置
            List<HighlightField> highlightFields = Arrays.asList(
                    new HighlightField("resourceName"),
                    new HighlightField("courseName"),
                    new HighlightField("teacherName"),
                    new HighlightField("courseDescription")
            );
            Highlight highlight = new Highlight(highlightFields);

            // 构建查询
            NativeQuery searchQuery = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                    .withHighlightQuery(new HighlightQuery(highlight, CourseResourceDocument.class))
                    .withPageable(PageRequest.of(0, request.getPageSize()))
                    .build();

            // 执行搜索
            SearchHits<CourseResourceDocument> searchHits = elasticsearchOperations.search(
                    searchQuery,
                    CourseResourceDocument.class
            );

            // 转换结果
            List<ResourceResponse> responses = searchHits.getSearchHits().stream()
                    .map(this::convertSearchHitToResponse)
                    .collect(Collectors.toList());

            return new SearchResponse(searchHits.getTotalHits(), responses);

        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResponse(0L, new ArrayList<>());
        }
    }

    @Override
    public boolean syncResourceToElasticsearch(Long resourceId) {
        CourseResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            return false;
        }
        syncToElasticsearch(resource);
        return true;
    }

    @Override
    public Map<String, Object> syncAllResourcesToElasticsearch() {
        Map<String, Object> result = new HashMap<>();

        if (elasticsearchOperations == null) {
            result.put("success", false);
            result.put("message", "Elasticsearch 未配置");
            return result;
        }

        try {
            List<CourseResource> allResources = resourceMapper.selectList(null);
            int successCount = 0;
            int failCount = 0;

            System.out.println("开始同步 " + allResources.size() + " 条资源到 Elasticsearch...");

            for (CourseResource resource : allResources) {
                try {
                    syncToElasticsearch(resource);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    System.err.println("同步失败: " + resource.getResourceName() + " - " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("total", allResources.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("message", "同步完成！成功: " + successCount + ", 失败: " + failCount);

            System.out.println("✅ 同步完成！成功: " + successCount + ", 失败: " + failCount);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
        }

        return result;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 将实体转换为响应DTO
     */
    private ResourceResponse convertToResponse(CourseResource resource) {
        ResourceResponse response = new ResourceResponse();
        response.setId(resource.getId());
        response.setResourceName(resource.getResourceName());
        response.setResourceType(resource.getResourceType());
        response.setResourceUrl(resource.getResourceUrl());
        response.setCreateTime(resource.getCreateTime());
        response.setCourseId(resource.getCourseId());

        // 查询课程信息
        Course course = courseMapper.selectById(resource.getCourseId());
        if (course != null) {
            response.setCourseName(course.getName());
        }

        return response;
    }

    /**
     * 将搜索结果转换为响应DTO
     */
    private ResourceResponse convertSearchHitToResponse(SearchHit<CourseResourceDocument> hit) {
        CourseResourceDocument doc = hit.getContent();
        ResourceResponse response = new ResourceResponse();

        response.setId(doc.getId());
        response.setResourceName(doc.getResourceName());
        response.setCourseName(doc.getCourseName());
        response.setTeacherName(doc.getTeacherName());
        response.setResourceType(doc.getResourceType());
        response.setScore(Double.valueOf(hit.getScore()));

        // 处理高亮
        Map<String, String> highlights = new HashMap<>();
        if (!hit.getHighlightFields().isEmpty()) {
            hit.getHighlightFields().forEach((field, values) -> {
                if (!values.isEmpty()) {
                    highlights.put(field, values.get(0));
                }
            });
        }
        response.setHighlights(highlights);

        return response;
    }

    /**
     * 同步资源到 Elasticsearch
     */
    private void syncToElasticsearch(CourseResource resource) {
        if (elasticsearchOperations == null) {
            System.out.println("⚠️ Elasticsearch 未配置，跳过索引同步");
            return;
        }

        try {
            // 查询课程信息
            Course course = courseMapper.selectById(resource.getCourseId());
            if (course == null) {
                System.err.println("❌ 课程不存在: courseId=" + resource.getCourseId());
                return;
            }

            // 查询教师信息
            Teacher teacher = null;
            QueryWrapper<CourseSchedule> scheduleQuery = new QueryWrapper<>();
            scheduleQuery.eq("course_id", resource.getCourseId()).last("LIMIT 1");
            CourseSchedule schedule = courseScheduleMapper.selectOne(scheduleQuery);

            if (schedule != null && schedule.getTeacherId() != null) {
                teacher = teacherMapper.selectById(schedule.getTeacherId());
            }

            // 构建 Elasticsearch 文档
            CourseResourceDocument document = new CourseResourceDocument();
            document.setId(resource.getId());
            document.setResourceName(resource.getResourceName());
            document.setResourceType(resource.getResourceType());
            document.setCourseId(course.getId());
            document.setCourseName(course.getName());
            document.setCourseDescription(course.getDescription());
            document.setCredit(course.getCredit());
            document.setTeacherName(teacher != null ? teacher.getName() : "未分配");

            // 转换时间格式
            if (resource.getCreateTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                document.setCreateTime(resource.getCreateTime().format(formatter));
            } else {
                document.setCreateTime(null);
            }

            // 保存到 Elasticsearch
            elasticsearchOperations.save(document);
            System.out.println("✅ 资源已同步到 Elasticsearch: " + resource.getResourceName());
        } catch (Exception e) {
            System.err.println("❌ Elasticsearch 同步失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
