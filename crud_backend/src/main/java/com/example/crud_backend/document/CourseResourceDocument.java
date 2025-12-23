package com.example.crud_backend.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * 课程资料 Elasticsearch 文档
 * 用于课程资料的智能搜索
 */
@Document(indexName = "course_resources")
@Setting(shards = 1, replicas = 0) // 开发环境配置
public class CourseResourceDocument {

    @Id
    private Long id;

    /**
     * 课程资料文件名
     * 使用 ik_max_word 分词器进行细粒度分词
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String resourceName;

    /**
     * 课程名称
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String courseName;

    /**
     * 课程描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String courseDescription;

    /**
     * 教师姓名
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String teacherName;

    /**
     * 文件类型（pdf, doc, ppt, mp4等）
     */
    @Field(type = FieldType.Keyword)
    private String resourceType;

    /**
     * 课程ID（用于关联查询）
     */
    @Field(type = FieldType.Long)
    private Long courseId;

    /**
     * 学分
     */
    @Field(type = FieldType.Integer)
    private Integer credit;

    /**
     * 上传时间（存储为字符串）
     */
    @Field(type = FieldType.Keyword)
    private String createTime;

    // 无参构造函数
    public CourseResourceDocument() {
    }

    // 全参构造函数
    public CourseResourceDocument(Long id, String resourceName, String courseName,
                                   String courseDescription, String teacherName,
                                   String resourceType, Long courseId, Integer credit,
                                   String createTime) {
        this.id = id;
        this.resourceName = resourceName;
        this.courseName = courseName;
        this.courseDescription = courseDescription;
        this.teacherName = teacherName;
        this.resourceType = resourceType;
        this.courseId = courseId;
        this.credit = credit;
        this.createTime = createTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Integer getCredit() {
        return credit;
    }

    public void setCredit(Integer credit) {
        this.credit = credit;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "CourseResourceDocument{" +
                "id=" + id +
                ", resourceName='" + resourceName + '\'' +
                ", courseName='" + courseName + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", courseId=" + courseId +
                ", credit=" + credit +
                ", createTime=" + createTime +
                '}';
    }
}
