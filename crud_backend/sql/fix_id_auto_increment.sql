-- ========================================
-- 修复ID字段：将雪花算法改为自增
-- 解决JavaScript精度丢失问题
-- ========================================

USE student;

-- 1. 修改 t_student_course 表的 id 字段为自增
-- 注意：如果表中有数据，需要先备份
ALTER TABLE `t_student_course` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(自增)';

-- 2. 修改 t_course_teacher 表的 id 字段为自增
ALTER TABLE `t_course_teacher` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(自增)';

-- 验证修改结果
SHOW CREATE TABLE t_student_course;
SHOW CREATE TABLE t_course_teacher;
