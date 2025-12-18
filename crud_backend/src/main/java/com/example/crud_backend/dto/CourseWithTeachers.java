package com.example.crud_backend.dto;

import com.example.crud_backend.entity.Course;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourseWithTeachers extends Course {
    private List<TeacherInfo> teachers;

    @Data
    public static class TeacherInfo {
        private Long id;
        private String name;
        private String title;
    }
}
