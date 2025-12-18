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

DROP TABLE IF EXISTS t_teacher;
-- 2. 教师表 (手动输入ID，不使用AUTO_INCREMENT)
CREATE TABLE t_teacher (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    title VARCHAR(50) COMMENT '职称',
    phone VARCHAR(20)
);

DROP TABLE IF EXISTS t_course;
-- 3. 课程基本信息表 (t_course)
CREATE TABLE t_course (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '课程名称',
    description TEXT,
    credit INT COMMENT '学分'
);

DROP TABLE IF EXISTS t_course_schedule;
-- 4. 课程排课/班级表 (核心表：解决"同一课程不同时间")
-- 这里我们将时间抽象为：week_day (周几) 和 section (第几节)
CREATE TABLE t_course_schedule (
    id BIGINT PRIMARY KEY,
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

DROP TABLE if EXISTS t_student_course;
-- 5. 选课记录表 (学生-课程关联)
CREATE TABLE t_student_course (
    id BIGINT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL, -- 对应 t_student 的 student_id (学号)
    schedule_id BIGINT NOT NULL, -- 对应排课表ID
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    score DECIMAL(5, 2) COMMENT '成绩',
    UNIQUE KEY uk_stu_schedule (student_id, schedule_id) -- 防止重复选同一门具体的课
);

-- 严格遵守你的规范，只添加课程资源表
DROP TABLE IF EXISTS `t_course_resource`;

CREATE TABLE `t_course_resource` (
    `id` BIGINT PRIMARY KEY,
    `course_id` BIGINT NOT NULL COMMENT '关联 t_course 的 id',
    `resource_name` VARCHAR(255) DEFAULT NULL,
    `resource_type` VARCHAR(50) COMMENT 'MP4, PDF',
    `resource_url` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES t_course (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `t_course_teacher`;

-- 6. 课程-教师关联表 (多对多关系：一门课程可以有多个老师，一个老师可以教多门课程)
CREATE TABLE `t_course_teacher` (
    `id` BIGINT PRIMARY KEY COMMENT '主键ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
    UNIQUE KEY `uk_course_teacher` (`course_id`, `teacher_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    FOREIGN KEY (`course_id`) REFERENCES `t_course` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`teacher_id`) REFERENCES `t_teacher` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程-教师关联表';

DROP TABLE IF EXISTS `t_notification`;

-- 7. 通知表 (站内消息、短信通知记录)
CREATE TABLE `t_notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID(用户名或学号)',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型: COURSE_REMIND, SELECTION_SUCCESS, COURSE_CHANGE, SYSTEM_NOTICE',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `course_id` BIGINT COMMENT '关联课程ID',
    `schedule_id` BIGINT COMMENT '关联排课ID',
    `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_is_read` (`is_read`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知表';