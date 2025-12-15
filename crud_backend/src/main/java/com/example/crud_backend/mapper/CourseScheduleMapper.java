package com.example.crud_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crud_backend.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {
    // 关联查询：查出排课信息的同时，查出课程名和教师名
    @Select("SELECT s.*, c.name as courseName, t.name as teacherName " +
            "FROM t_course_schedule s " +
            "LEFT JOIN t_course c ON s.course_id = c.id " +
            "LEFT JOIN t_teacher t ON s.teacher_id = t.id")
    List<CourseSchedule> selectScheduleWithDetails();
    
    // 根据学生ID查询课表 (用于选课界面回显)
    @Select("SELECT s.*, c.name as courseName, t.name as teacherName " +
            "FROM t_student_course sc " +
            "JOIN t_course_schedule s ON sc.schedule_id = s.id " +
            "LEFT JOIN t_course c ON s.course_id = c.id " +
            "LEFT JOIN t_teacher t ON s.teacher_id = t.id " +
            "WHERE sc.student_id = #{studentId}")
    List<CourseSchedule> selectScheduleByStudentId(String studentId);
}
