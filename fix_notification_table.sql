-- ========================================
-- 修复通知表结构和RabbitMQ问题
-- ========================================

USE student;

-- 1. 删除旧的通知表(如果存在联合主键的错误版本)
DROP TABLE IF EXISTS `t_notification`;

-- 2. 创建正确的通知表(使用自增主键)
CREATE TABLE `t_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(自增)',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表 - 支持重复选课退课通知';

-- 3. 检查表结构是否正确
SHOW CREATE TABLE t_notification;

-- 4. 查看现有通知数据
SELECT COUNT(*) as total_notifications FROM t_notification;

-- 5. 清理测试数据(可选 - 如果需要重新开始)
-- TRUNCATE TABLE t_notification;

SELECT '✅ 通知表结构修复完成' as status;
