package com.example.crud_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crud_backend.dto.CourseWithTeachers;
import com.example.crud_backend.dto.request.SearchRequest;
import com.example.crud_backend.dto.response.SearchResponse;
import com.example.crud_backend.entity.Course;
import com.example.crud_backend.service.ICourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程控制器
 * 职责：接收HTTP请求，调用Service层，返回响应
 */
@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private ICourseService courseService;

    /**
     * 分页查询课程（带教师信息）
     */
    @GetMapping("/page")
    public Page<CourseWithTeachers> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search) {
        return courseService.getCoursesWithTeachers(pageNum, pageSize, search);
    }

    /**
     * 保存或更新课程（带教师关联）
     */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody Map<String, Object> request) {
        return courseService.saveCourseWithTeachers(request);
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return courseService.deleteCourseWithRelations(id);
    }

    /**
     * 获取所有课程（用于下拉选择）
     */
    @GetMapping("/all")
    public List<Course> getAll() {
        return courseService.list();
    }

    /**
     * 为课程添加教师
     */
    @PostMapping("/{courseId}/teacher/{teacherId}")
    public Map<String, Object> addTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        return courseService.addTeacherToCourse(courseId, teacherId);
    }

    /**
     * 删除课程的某个教师
     */
    @DeleteMapping("/{courseId}/teacher/{teacherId}")
    public Map<String, Object> removeTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        return courseService.removeTeacherFromCourse(courseId, teacherId);
    }

    /**
     * 搜索课程资料 - Elasticsearch 智能搜索
     */
    @GetMapping("/search-resources")
    public Map<String, Object> searchResources(
            @RequestParam String keyword,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long courseId) {

        SearchRequest request = new SearchRequest();
        request.setKeyword(keyword);
        request.setResourceType(resourceType);
        request.setCourseId(courseId);

        SearchResponse response = courseService.searchCourseResources(request);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("code", 200);
        result.put("msg", "搜索成功");
        result.put("data", response.getData());
        result.put("total", response.getTotal());
        return result;
    }
}
