package com.example.crud_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_course_schedule")
public class CourseSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long teacherId;
    private String semester;
    private Integer weekDay; // 1-7
    private Integer sectionStart; // 开始节次
    private Integer sectionEnd; // 结束节次
    private String location;
    private Integer maxCapacity;
    private Integer currentCount;
}