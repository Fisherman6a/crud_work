package com.example.crud_backend.entity.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.Data;

@Data
@Document(indexName = "course_docs")
public class CourseDoc {
    @Id
    private String id; // 对应数据库的附件ID或文件路径

    @Field(type = FieldType.Text, analyzer = "ik_max_word") // 假设安装了IK分词器，否则用 standard
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content; // 提取的文件内容

    @Field(type = FieldType.Keyword)
    private String fileUrl;

    @Field(type = FieldType.Long)
    private Long courseId;
}