-- DDL.sql: 数据库结构定义 (已整合 modify.sql)

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `smartdoc` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `smartdoc`;

-- 2. 清理旧表 (初始化用)
DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `sys_budget`;
DROP TABLE IF EXISTS `sys_chat_log`;
DROP TABLE IF EXISTS `invoice_record`;
DROP TABLE IF EXISTS `sys_user`;

-- 3. 系统用户表
CREATE TABLE `sys_user` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `username` varchar(50) NOT NULL COMMENT '用户名 (登录账号)',
                            `password` varchar(100) NOT NULL COMMENT '密码',
                            `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
                            `role` varchar(20) DEFAULT 'user' COMMENT '角色权限 (admin/user)',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`) USING BTREE COMMENT '用户名唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- 4. 智能票据归档表 (已包含 user_id, status, is_deleted, is_anomaly 等字段)
CREATE TABLE `invoice_record` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `user_id` bigint(20) DEFAULT NULL COMMENT '所属用户ID',
                                  `merchant_name` varchar(255) DEFAULT NULL COMMENT '商户名称',
                                  `item_name` varchar(255) DEFAULT NULL COMMENT '项目名称/商品明细',
                                  `invoice_code` varchar(50) DEFAULT NULL COMMENT '发票号码',
                                  `amount` double(10,2) DEFAULT NULL COMMENT '金额',
                                  `date` varchar(20) DEFAULT NULL COMMENT '开票日期',
                                  `category` varchar(50) DEFAULT NULL COMMENT '智能分类',
                                  `status` tinyint(1) DEFAULT 0 COMMENT '审批状态 (0=草稿, 1=待审核, 2=已通过, 3=已驳回)',
                                  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审批驳回原因',
                                  `is_anomaly` tinyint(1) DEFAULT 0 COMMENT '是否异常(0否 1是)',
                                  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标记(0=正常, 1=已删除)',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  INDEX `idx_user_invoice` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能票据归档表';

-- 5. AI对话记录表 (已包含 session_id)
CREATE TABLE `sys_chat_log` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                `session_id` varchar(64) NOT NULL DEFAULT 'default' COMMENT '会话ID',
                                `role` varchar(10) NOT NULL COMMENT '角色: user / ai',
                                `content` text NOT NULL COMMENT '聊天内容',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                INDEX `idx_user` (`user_id`),
                                INDEX `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

-- 6. 预算管理表
CREATE TABLE `sys_budget` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                              `category` varchar(50) NOT NULL COMMENT '分类名称',
                              `limit_amount` double(10,2) NOT NULL COMMENT '预算限额',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_user_category` (`user_id`, `category`) COMMENT '防止同一用户对同一分类设多条预算'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算管理表';

-- 7. 操作审计日志表
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