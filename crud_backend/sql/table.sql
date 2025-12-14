use student;
-- 1. 新增用户表
CREATE TABLE `t_user` (
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(50) NOT NULL COMMENT '密码',
    `role` varchar(20) NOT NULL COMMENT '角色: ADMIN / USER',
    PRIMARY KEY (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 初始化数据
INSERT INTO t_user VALUES ('admin', '123456', 'ADMIN');

INSERT INTO t_user VALUES ('user', '123456', 'USER');

-- 2. 修改学生表 (将学号设为主键，或者保留ID但给学号加唯一索引)
-- 建议方案：为了符合"学号做主键"的要求，我们重建表结构
DROP TABLE IF EXISTS `t_student`;

CREATE TABLE `t_student` (
    `student_number` varchar(20) NOT NULL COMMENT '学号(主键)',
    `name` varchar(50) DEFAULT NULL,
    `gender` varchar(10) DEFAULT NULL,
    `age` int DEFAULT NULL,
    `class_name` varchar(50) DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`student_number`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- 1. 修改系统用户表：添加关联字段
-- 这样登录后，如果是学生角色，就知道对应的 student_id 是多少
ALTER TABLE t_user
ADD COLUMN related_id VARCHAR(50) COMMENT '关联ID(学生ID或教师ID)';

-- 2. 教师表
CREATE TABLE t_teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    title VARCHAR(50) COMMENT '职称',
    phone VARCHAR(20)
);

-- 3. 课程基本信息表 (只存课程元数据，不存时间)
CREATE TABLE t_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '课程名称',
    description TEXT,
    credit INT COMMENT '学分'
);

-- 4. 课程排课/班级表 (核心表：解决"同一课程不同时间")
-- 这里我们将时间抽象为：week_day (周几) 和 section (第几节)
CREATE TABLE t_course_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '关联课程ID',
    teacher_id BIGINT NOT NULL COMMENT '关联教师ID',
    semester VARCHAR(20) DEFAULT '2025-1' COMMENT '学期',
    week_day INT NOT NULL COMMENT '周几 (1-7)',
    section_start INT NOT NULL COMMENT '开始节次 (如 1 代表第一节)',
    section_end INT NOT NULL COMMENT '结束节次 (如 2)',
    location VARCHAR(50) COMMENT '教室',
    max_capacity INT DEFAULT 50 COMMENT '最大容量',
    current_count INT DEFAULT 0 COMMENT '已选人数',
    FOREIGN KEY (course_id) REFERENCES t_course (id),
    FOREIGN KEY (teacher_id) REFERENCES t_teacher (id)
);

-- 5. 选课记录表 (学生-课程关联)
CREATE TABLE t_student_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL, -- 对应 t_student 的 student_id (学号)
    schedule_id BIGINT NOT NULL, -- 对应排课表ID
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    score DECIMAL(5, 2) COMMENT '成绩',
    UNIQUE KEY uk_stu_schedule (student_id, schedule_id) -- 防止重复选同一门具体的课
);