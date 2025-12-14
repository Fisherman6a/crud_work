package com.example.crud_backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface TimetableMapper {
    /**
     * 查询某学生的详细课表（包含课程名、老师名、地点、时间）
     */
    List<Map<String, Object>> selectTimetableByStudentId(@Param("studentId") String studentId);

    /**
     * 查询所有可选排课信息（用于选课页面，展示课程名和老师名）
     */
    List<Map<String, Object>> selectAllScheduleDetails();
}