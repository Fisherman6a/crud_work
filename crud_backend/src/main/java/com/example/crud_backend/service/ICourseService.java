package com.example.crud_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.crud_backend.dto.CourseWithTeachers;
import com.example.crud_backend.dto.request.SearchRequest;
import com.example.crud_backend.dto.response.SearchResponse;
import com.example.crud_backend.entity.Course;

import java.util.Map;

/**
 * 课程服务接口
 */
public interface ICourseService extends IService<Course> {

    /**
     * 分页查询课程（带教师信息）
     */
    Page<CourseWithTeachers> getCoursesWithTeachers(int pageNum, int pageSize, String search);

    /**
     * 保存或更新课程（带教师关联）
     */
    Map<String, Object> saveCourseWithTeachers(Map<String, Object> request);

    /**
     * 删除课程（级联删除关联数据）
     */
    boolean deleteCourseWithRelations(Long id);

    /**
     * 为课程添加教师
     */
    Map<String, Object> addTeacherToCourse(Long courseId, Long teacherId);

    /**
     * 从课程中移除教师
     */
    Map<String, Object> removeTeacherFromCourse(Long courseId, Long teacherId);

    /**
     * 搜索课程资源（Elasticsearch）
     */
    SearchResponse searchCourseResources(SearchRequest request);
}
