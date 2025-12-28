package com.example.crud_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.crud_backend.document.CourseResourceDocument;
import com.example.crud_backend.dto.CourseWithTeachers;
import com.example.crud_backend.dto.request.SearchRequest;
import com.example.crud_backend.dto.response.ResourceResponse;
import com.example.crud_backend.dto.response.SearchResponse;
import com.example.crud_backend.entity.Course;
import com.example.crud_backend.entity.CourseTeacher;
import com.example.crud_backend.entity.Teacher;
import com.example.crud_backend.mapper.CourseMapper;
import com.example.crud_backend.mapper.CourseTeacherMapper;
import com.example.crud_backend.mapper.TeacherMapper;
import com.example.crud_backend.service.ICourseService;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程服务实现类
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<CourseWithTeachers> getCoursesWithTeachers(int pageNum, int pageSize, String search) {
        Page<Course> coursePage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(search)) {
            wrapper.like(Course::getName, search);
        }

        Page<Course> resultPage = this.page(coursePage, wrapper);

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

    @Override
    @Transactional
    public Map<String, Object> saveCourseWithTeachers(Map<String, Object> request) {
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
            Course existing = this.getById(courseId);

            // 从前端获取 originalId
            Long originalId = null;
            if (request.containsKey("originalId") && request.get("originalId") != null) {
                originalId = Long.valueOf(request.get("originalId").toString());
            }

            // 判断是否为新增模式
            boolean isAddMode = (originalId == null);

            if (isAddMode && existing != null) {
                result.put("success", false);
                result.put("message", "课程ID " + courseId + " 已存在，请使用其他ID");
                return result;
            }

            // 保存课程
            boolean saveSuccess = this.saveOrUpdate(course);
            if (!saveSuccess) {
                result.put("success", false);
                result.put("message", "课程保存失败");
                return result;
            }

            // 处理教师关联
            if (request.containsKey("teacherIds")) {
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

    @Override
    @Transactional
    public boolean deleteCourseWithRelations(Long id) {
        // 删除课程-教师关联
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, id);
        courseTeacherMapper.delete(wrapper);

        // 删除课程
        return this.removeById(id);
    }

    @Override
    public Map<String, Object> addTeacherToCourse(Long courseId, Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        // 验证课程和教师是否存在
        Course course = this.getById(courseId);
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

    @Override
    public Map<String, Object> removeTeacherFromCourse(Long courseId, Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getTeacherId, teacherId);

        int deleted = courseTeacherMapper.delete(wrapper);

        result.put("success", deleted > 0);
        return result;
    }

    @Override
    public SearchResponse searchCourseResources(SearchRequest request) {
        if (elasticsearchOperations == null) {
            return new SearchResponse(0L, new ArrayList<>());
        }

        try {
            // 构建查询
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // 多字段匹配
            if (StringUtils.hasText(request.getKeyword())) {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                        .query(request.getKeyword())
                        .fields("resourceName^3.0", "courseName^2.0", "teacherName^1.5", "courseDescription^1.0")
                        .type(TextQueryType.BestFields)
                        .fuzziness("AUTO")
                );
                boolQueryBuilder.must(Query.of(q -> q.multiMatch(multiMatchQuery)));
            }

            // 文件类型过滤
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

            // 构建高亮
            List<HighlightField> highlightFieldsList = Arrays.asList(
                    new HighlightField("resourceName"),
                    new HighlightField("courseName"),
                    new HighlightField("teacherName")
            );
            Highlight highlight = new Highlight(highlightFieldsList);

            // 构建查询
            NativeQuery searchQuery = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                    .withHighlightQuery(new HighlightQuery(highlight, CourseResourceDocument.class))
                    .withPageable(PageRequest.of(0, request.getPageSize()))
                    .build();

            // 执行搜索
            SearchHits<CourseResourceDocument> searchHits =
                    elasticsearchOperations.search(searchQuery, CourseResourceDocument.class);

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

    // ========== 私有辅助方法 ==========

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
        response.setCourseId(doc.getCourseId());
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
}
