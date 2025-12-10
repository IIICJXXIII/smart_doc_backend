USE `smartdoc`;

-- 1. 增加 user_id 字段
ALTER TABLE `invoice_record` ADD COLUMN `user_id` BIGINT COMMENT '所属用户ID';

-- 2. (可选) 把现有的旧数据全部归属给管理员(id=1)，防止数据丢失或查不到
UPDATE `invoice_record` SET `user_id` = 1 WHERE `user_id` IS NULL;

USE `smartdoc`;

CREATE TABLE `sys_chat_log` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                `role` varchar(10) NOT NULL COMMENT '角色: user / ai',
                                `content` text NOT NULL COMMENT '聊天内容',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

USE `smartdoc`;

-- 1. 增加 session_id 字段
ALTER TABLE `sys_chat_log` ADD COLUMN `session_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '会话ID';

-- 2. 增加索引，加快查询速度
ALTER TABLE `sys_chat_log` ADD INDEX `idx_session` (`session_id`);

USE `smartdoc`;

CREATE TABLE `sys_budget` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                              `category` varchar(50) NOT NULL COMMENT '分类名称',
                              `limit_amount` double(10,2) NOT NULL COMMENT '预算限额',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_user_category` (`user_id`, `category`) -- 防止同一用户对同一分类设多条预算
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算管理表';

