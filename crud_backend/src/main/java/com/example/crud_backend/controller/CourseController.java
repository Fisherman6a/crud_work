package com.example.crud_backend.controller;

import com.example.crud_backend.entity.es.CourseDoc;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/course")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // 上传课程附件
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, @RequestParam("courseId") Long courseId)
            throws Exception {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        // 1. 上传到 MinIO
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        }

        // 2. 提取文件内容 (Tika)
        Tika tika = new Tika();
        String content = tika.parseToString(file.getInputStream());

        // 3. 存入 Elasticsearch
        CourseDoc doc = new CourseDoc();
        doc.setId(fileName);
        doc.setTitle(file.getOriginalFilename());
        doc.setContent(content); // 索引全文内容
        doc.setCourseId(courseId);
        doc.setFileUrl(fileName);
        elasticsearchOperations.save(doc);

        return fileName;
    }

    // 全文搜索附件
    @GetMapping("/search")
    public List<CourseDoc> search(@RequestParam String keyword) {
        Criteria criteria = new Criteria("content").contains(keyword)
                .or(new Criteria("title").contains(keyword));

        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<CourseDoc> searchHits = elasticsearchOperations.search(query, CourseDoc.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    // 获取预览/播放链接 (支持视频、音频在线播放，文档下载/浏览器预览)
    @GetMapping("/preview/{fileName}")
    public Map<String, String> getPreviewUrl(@PathVariable String fileName) throws Exception {
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(fileName)
                        .expiry(24 * 60 * 60) // 链接有效期
                        .build());
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        // 前端拿到URL后：
        // 1. 如果是 mp4/mp3 -> <video src="url">
        // 2. 如果是 pdf -> iframe src="url" (浏览器原生支持)
        // 3. 如果是 doc/ppt -> 建议接入微软Office Online预览或前端插件
        return result;
    }
}