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

USE `smartdoc`;
ALTER TABLE `invoice_record` ADD COLUMN `is_anomaly` TINYINT(1) DEFAULT 0 COMMENT '是否异常(0否 1是)';

USE `smartdoc`;

CREATE TABLE `sys_operation_log` (
                                     `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                     `user_id` bigint(20) NOT NULL,
                                     `operation` varchar(50) NOT NULL COMMENT '操作类型(如: 删除发票)',
                                     `detail` varchar(255) DEFAULT NULL COMMENT '详情(如: 发票ID:105)',
                                     `ip_address` varchar(50) DEFAULT NULL COMMENT '操作IP',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`id`),
                                     INDEX `idx_user_op` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

USE `smartdoc`;

-- 1. 增加删除标记 (0=正常, 1=已删除)
ALTER TABLE `invoice_record` ADD COLUMN `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记';

-- 2. 刷新旧数据，确保都是 0
UPDATE `invoice_record` SET `is_deleted` = 0 WHERE `is_deleted` IS NULL;

USE `smartdoc`;

-- 1. 增加状态字段 (0=草稿, 1=待审核, 2=已通过, 3=已驳回)
ALTER TABLE `invoice_record` ADD COLUMN `status` TINYINT(1) DEFAULT 0 COMMENT '审批状态';

-- 2. 增加审批意见字段
ALTER TABLE `invoice_record` ADD COLUMN `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审批驳回原因';

-- 3. 初始化旧数据为 "已通过" (假设旧数据都有效)
UPDATE `invoice_record` SET `status` = 2 WHERE `status` = 0;

