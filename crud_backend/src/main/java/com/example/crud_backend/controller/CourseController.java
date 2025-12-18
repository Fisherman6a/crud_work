package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crud_backend.dto.CourseWithTeachers;
import com.example.crud_backend.entity.Course;
import com.example.crud_backend.entity.CourseTeacher;
import com.example.crud_backend.entity.Teacher;
import com.example.crud_backend.entity.es.CourseDoc;
import com.example.crud_backend.mapper.CourseTeacherMapper;
import com.example.crud_backend.mapper.TeacherMapper;
import com.example.crud_backend.service.ICourseService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.apache.tika.Tika;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/course")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private ICourseService courseService;

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${minio.bucket-name:course-bucket}")
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

    // ========== Course CRUD 接口 ==========

    // 分页查询课程（带教师信息）
    @GetMapping("/page")
    public Page<CourseWithTeachers> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search) {

        Page<Course> coursePage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(search)) {
            wrapper.like(Course::getName, search);
        }

        Page<Course> resultPage = courseService.page(coursePage, wrapper);

        // 转换为带教师信息的DTO
        Page<CourseWithTeachers> dtoPage = new Page<>();
        BeanUtils.copyProperties(resultPage, dtoPage, "records");

        List<CourseWithTeachers> dtoList = resultPage.getRecords().stream().map(course -> {
            CourseWithTeachers dto = new CourseWithTeachers();
            BeanUtils.copyProperties(course, dto);

            // 查询该课程的所有教师
            LambdaQueryWrapper<CourseTeacher> ctWrapper = new LambdaQueryWrapper<>();
            ctWrapper.eq(CourseTeacher::getCourseId, course.getId());
            List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(ctWrapper);

            List<CourseWithTeachers.TeacherInfo> teachers = courseTeachers.stream().map(ct -> {
                Teacher teacher = teacherMapper.selectById(ct.getTeacherId());
                if (teacher != null) {
                    CourseWithTeachers.TeacherInfo info = new CourseWithTeachers.TeacherInfo();
                    info.setId(teacher.getId());
                    info.setName(teacher.getName());
                    info.setTitle(teacher.getTitle());
                    return info;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            dto.setTeachers(teachers);
            return dto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    // 保存或更新课程（带教师关联）
    @PostMapping("/save")
    @Transactional
    public Map<String, Object> save(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 提取课程信息
            Course course = new Course();
            Long courseId = null;
            if (request.get("id") != null && !request.get("id").toString().isEmpty()) {
                courseId = Long.valueOf(request.get("id").toString());
                course.setId(courseId);
            }
            course.setName((String) request.get("name"));
            course.setDescription((String) request.get("description"));
            if (request.get("credit") != null) {
                course.setCredit(Integer.valueOf(request.get("credit").toString()));
            }

            // ID必填验证
            if (courseId == null) {
                result.put("success", false);
                result.put("message", "课程ID不能为空");
                return result;
            }

            // 检查ID是否已存在
            Course existing = courseService.getById(courseId);

            // 从前端获取 originalId（编辑模式下，originalId 就是当前记录的ID）
            Long originalId = null;
            if (request.containsKey("originalId") && request.get("originalId") != null) {
                originalId = Long.valueOf(request.get("originalId").toString());
            }

            // 判断是否为新增模式：originalId 为 null 表示新增
            boolean isAddMode = (originalId == null);

            if (isAddMode && existing != null) {
                // 新增模式下，如果ID已存在，拒绝
                result.put("success", false);
                result.put("message", "课程ID " + courseId + " 已存在，请使用其他ID");
                return result;
            }

            // 保存课程
            boolean saveSuccess = courseService.saveOrUpdate(course);
            if (!saveSuccess) {
                result.put("success", false);
                result.put("message", "课程保存失败");
                return result;
            }

            // 处理教师关联
            if (request.containsKey("teacherIds")) {
                // Convert List<Integer> to List<Long>
                List<?> rawTeacherIds = (List<?>) request.get("teacherIds");
                List<Long> teacherIds = rawTeacherIds.stream()
                        .map(id -> Long.valueOf(id.toString()))
                        .collect(Collectors.toList());

                // 删除旧的关联
                LambdaQueryWrapper<CourseTeacher> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(CourseTeacher::getCourseId, course.getId());
                courseTeacherMapper.delete(deleteWrapper);

                // 添加新的关联
                if (teacherIds != null && !teacherIds.isEmpty()) {
                    for (Long teacherId : teacherIds) {
                        // 验证教师是否存在
                        Teacher teacher = teacherMapper.selectById(teacherId);
                        if (teacher != null) {
                            CourseTeacher ct = new CourseTeacher();
                            ct.setCourseId(course.getId());
                            ct.setTeacherId(teacherId);
                            courseTeacherMapper.insert(ct);
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("courseId", course.getId());
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
            return result;
        }
    }

    // 删除课程
    @DeleteMapping("/{id}")
    @Transactional
    public boolean delete(@PathVariable Long id) {
        // 删除课程-教师关联
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, id);
        courseTeacherMapper.delete(wrapper);

        // 删除课程
        return courseService.removeById(id);
    }

    // 获取所有课程（用于下拉选择）
    @GetMapping("/all")
    public List<Course> getAll() {
        return courseService.list();
    }

    // 为课程添加教师
    @PostMapping("/{courseId}/teacher/{teacherId}")
    public Map<String, Object> addTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        // 验证课程和教师是否存在
        Course course = courseService.getById(courseId);
        Teacher teacher = teacherMapper.selectById(teacherId);

        if (course == null) {
            result.put("success", false);
            result.put("message", "课程不存在");
            return result;
        }

        if (teacher == null) {
            result.put("success", false);
            result.put("message", "教师不存在");
            return result;
        }

        // 检查是否已经关联
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, courseId)
               .eq(CourseTeacher::getTeacherId, teacherId);
        CourseTeacher existing = courseTeacherMapper.selectOne(wrapper);

        if (existing != null) {
            result.put("success", false);
            result.put("message", "该教师已经是该课程的任课老师");
            return result;
        }

        // 添加关联
        CourseTeacher ct = new CourseTeacher();
        ct.setCourseId(courseId);
        ct.setTeacherId(teacherId);
        courseTeacherMapper.insert(ct);

        result.put("success", true);
        return result;
    }

    // 删除课程的某个教师
    @DeleteMapping("/{courseId}/teacher/{teacherId}")
    public Map<String, Object> removeTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, courseId)
               .eq(CourseTeacher::getTeacherId, teacherId);

        int deleted = courseTeacherMapper.delete(wrapper);

        result.put("success", deleted > 0);
        return result;
    }
}