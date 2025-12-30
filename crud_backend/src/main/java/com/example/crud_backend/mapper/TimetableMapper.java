package com.example.crud_backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface TimetableMapper {
    /**
     * 查询某学生的详细课表（包含课程名、老师名、地点、时间）
     */
    @Select("SELECT " +
            "sc.id as selection_id, " +
            "s.id as schedule_id, " +
            "s.course_id, " +
            "s.week_day, " +
            "s.section_start, " +
            "s.section_end, " +
            "s.location, " +
            "s.current_count, " +
            "s.max_capacity, " +
            "c.name as course_name, " +
            "c.credit, " +
            "c.description, " +
            "t.name as teacher_name " +
            "FROM t_student_course sc " +
            "JOIN t_course_schedule s ON sc.schedule_id = s.id " +
            "JOIN t_course c ON s.course_id = c.id " +
            "JOIN t_teacher t ON s.teacher_id = t.id " +
            "WHERE sc.student_id = #{studentId}")
    List<Map<String, Object>> selectTimetableByStudentId(@Param("studentId") String studentId);

    /**
     * 查询所有可选排课信息（用于选课页面，展示课程名和老师名）
     */
    @Select("SELECT " +
            "s.id as schedule_id, " +
            "s.course_id, " +
            "s.week_day, " +
            "s.section_start, " +
            "s.section_end, " +
            "s.location, " +
            "s.current_count, " +
            "s.max_capacity, " +
            "c.name as course_name, " +
            "c.credit, " +
            "c.description, " +
            "t.name as teacher_name " +
            "FROM t_course_schedule s " +
            "JOIN t_course c ON s.course_id = c.id " +
            "JOIN t_teacher t ON s.teacher_id = t.id")
    List<Map<String, Object>> selectAllScheduleDetails();
}