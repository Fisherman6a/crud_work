-- ========================================
-- 完整SQL建表脚本 - 教务管理系统
-- 数据库: student
-- 编码: UTF8MB4
-- 引擎: InnoDB
-- 创建时间: 2025-12
-- ========================================

-- 使用数据库
USE student;

-- ========================================
-- 1. 用户表 (t_user)
-- 用于登录认证，支持管理员和普通用户
-- ========================================
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `username` VARCHAR(50) NOT NULL COMMENT '用户名(主键)',
    `password` VARCHAR(50) NOT NULL COMMENT '密码',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: ADMIN(管理员) / USER(学生)',
    `related_id` VARCHAR(50) COMMENT '关联ID(学生学号或教师ID)',
    PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始化管理员账号
INSERT INTO `t_user` (`username`, `password`, `role`) VALUES ('admin', '123456', 'ADMIN');

-- ========================================
-- 2. 学生表 (t_student)
-- 学号作为主键，记录学生基本信息
-- ========================================
DROP TABLE IF EXISTS `t_student`;
CREATE TABLE `t_student` (
    `student_number` VARCHAR(20) NOT NULL COMMENT '学号(主键)',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
    `age` INT DEFAULT NULL COMMENT '年龄',
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级名称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号(用于短信通知)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`student_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- ========================================
-- 3. 教师表 (t_teacher)
-- 教师ID手动输入，不使用自增
-- ========================================
DROP TABLE IF EXISTS `t_teacher`;
CREATE TABLE `t_teacher` (
    `id` BIGINT NOT NULL COMMENT '教师ID(手动输入)',
    `name` VARCHAR(50) NOT NULL COMMENT '教师姓名',
    `title` VARCHAR(50) DEFAULT NULL COMMENT '职称(助教/讲师/副教授/教授)',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- ========================================
-- 4. 课程基本信息表 (t_course)
-- 课程ID手动输入，记录课程基础信息
-- ========================================
DROP TABLE IF EXISTS `t_course`;
CREATE TABLE `t_course` (
    `id` BIGINT NOT NULL COMMENT '课程ID(手动输入)',
    `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `description` TEXT COMMENT '课程描述',
    `credit` INT DEFAULT NULL COMMENT '学分',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程基本信息表';

-- ========================================
-- 5. 课程-教师关联表 (t_course_teacher)
-- 多对多关系：一门课程可以有多个教师，一个教师可以教多门课程
-- ========================================
DROP TABLE IF EXISTS `t_course_teacher`;
CREATE TABLE `t_course_teacher` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(自增)',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `teacher_id` BIGINT NOT NULL COMMENT '教师ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_teacher` (`course_id`, `teacher_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    CONSTRAINT `fk_ct_course` FOREIGN KEY (`course_id`) REFERENCES `t_course` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ct_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `t_teacher` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程-教师关联表';

-- ========================================
-- 6. 课程排课表 (t_course_schedule)
-- 核心表：记录课程的具体排课信息（时间、地点、容量）
-- 一门课程可以有多个排课（不同时间段/不同班级）
-- ========================================
DROP TABLE IF EXISTS `t_course_schedule`;
CREATE TABLE `t_course_schedule` (
    `id` BIGINT NOT NULL COMMENT '排课ID(手动输入)',
    `course_id` BIGINT NOT NULL COMMENT '关联课程ID',
    `teacher_id` BIGINT NOT NULL COMMENT '关联教师ID',
    `semester` VARCHAR(20) DEFAULT '2025-1' COMMENT '学期(如: 2025-1)',
    `week_day` INT NOT NULL COMMENT '周几(1-7: 周一到周日)',
    `section_start` INT NOT NULL COMMENT '开始节次(1-8)',
    `section_end` INT NOT NULL COMMENT '结束节次(1-8)',
    `location` VARCHAR(50) DEFAULT NULL COMMENT '上课地点(教室)',
    `max_capacity` INT DEFAULT 50 COMMENT '最大容量',
    `current_count` INT DEFAULT 0 COMMENT '当前已选人数',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_semester` (`semester`),
    CONSTRAINT `fk_cs_course` FOREIGN KEY (`course_id`) REFERENCES `t_course` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cs_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `t_teacher` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程排课表';

-- ========================================
-- 7. 学生选课记录表 (t_student_course)
-- 记录学生的选课情况，关联到具体的排课
-- ========================================
DROP TABLE IF EXISTS `t_student_course`;
CREATE TABLE `t_student_course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(自增)',
    `student_id` VARCHAR(50) NOT NULL COMMENT '学生学号',
    `schedule_id` BIGINT NOT NULL COMMENT '排课ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    `score` DECIMAL(5,2) DEFAULT NULL COMMENT '成绩',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stu_schedule` (`student_id`, `schedule_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_schedule_id` (`schedule_id`),
    CONSTRAINT `fk_sc_student` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`student_number`) ON DELETE CASCADE,
    CONSTRAINT `fk_sc_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `t_course_schedule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生选课记录表';

-- ========================================
-- 8. 课程资源表 (t_course_resource)
-- 存储课程的附件资源(PDF, Word, PPT, 音视频等)
-- 文件存储在MinIO，此表记录元数据
-- ========================================
DROP TABLE IF EXISTS `t_course_resource`;
CREATE TABLE `t_course_resource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_id` BIGINT NOT NULL COMMENT '关联课程ID',
    `resource_name` VARCHAR(255) NOT NULL COMMENT '资源名称',
    `resource_type` VARCHAR(50) DEFAULT NULL COMMENT '资源类型(pdf/doc/ppt/mp4/mp3等)',
    `resource_url` VARCHAR(500) NOT NULL COMMENT 'MinIO文件路径',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    CONSTRAINT `fk_cr_course` FOREIGN KEY (`course_id`) REFERENCES `t_course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资源表';

-- ========================================
-- 9. 通知表 (t_notification)
-- 记录站内消息和短信通知
-- 支持多种通知类型：选课成功、课程变更、课程提醒等
-- ========================================
DROP TABLE IF EXISTS `t_notification`;
CREATE TABLE `t_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID(用户名或学号)',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型: COURSE_REMIND/SELECTION_SUCCESS/COURSE_CHANGE/SYSTEM_NOTICE',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID',
    `schedule_id` BIGINT DEFAULT NULL COMMENT '关联排课ID',
    `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ========================================
-- 示例数据插入（可选）
-- ========================================

-- 插入示例教师
INSERT INTO `t_teacher` (`id`, `name`, `title`, `phone`) VALUES
(1001, '张三', '教授', '13800138001'),
(1002, '李四', '副教授', '13800138002'),
(1003, '王五', '讲师', '13800138003');

-- 插入示例课程
INSERT INTO `t_course` (`id`, `name`, `description`, `credit`) VALUES
(101, 'Java程序设计', 'Java语言基础与面向对象编程', 4),
(102, '数据结构与算法', '常用数据结构和算法设计', 4),
(103, '数据库原理', '关系型数据库原理与SQL语言', 3),
(104, 'Web开发技术', '前端与后端Web开发技术', 3);

-- 插入示例课程-教师关联
INSERT INTO `t_course_teacher` (`id`, `course_id`, `teacher_id`) VALUES
(1, 101, 1001),
(2, 102, 1002),
(3, 103, 1003),
(4, 104, 1001);

-- 插入示例排课
INSERT INTO `t_course_schedule` (`id`, `course_id`, `teacher_id`, `semester`, `week_day`, `section_start`, `section_end`, `location`, `max_capacity`, `current_count`) VALUES
(10001, 101, 1001, '2025-1', 1, 1, 2, '教学楼A101', 50, 0),
(10002, 101, 1001, '2025-1', 3, 3, 4, '教学楼A101', 50, 0),
(10003, 102, 1002, '2025-1', 2, 1, 2, '教学楼B201', 60, 0),
(10004, 103, 1003, '2025-1', 4, 5, 6, '实验楼C301', 40, 0),
(10005, 104, 1001, '2025-1', 5, 3, 4, '实验楼C302', 40, 0);

-- 插入示例学生
INSERT INTO `t_student` (`student_number`, `name`, `gender`, `age`, `class_name`, `phone`) VALUES
('2021001', '张小明', '男', 20, '计算机21-1班', '13900139001'),
('2021002', '李小红', '女', 19, '计算机21-1班', '13900139002'),
('2021003', '王小刚', '男', 20, '计算机21-2班', '13900139003');

-- 插入示例用户（学生登录账号）
INSERT INTO `t_user` (`username`, `password`, `role`, `related_id`) VALUES
('2021001', '123456', 'USER', '2021001'),
('2021002', '123456', 'USER', '2021002'),
('2021003', '123456', 'USER', '2021003');
