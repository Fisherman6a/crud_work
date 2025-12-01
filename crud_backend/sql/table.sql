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