-- 1. 如果不存在则创建数据库 smartdoc
CREATE DATABASE IF NOT EXISTS `smartdoc` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `smartdoc`;

-- 2. 如果存在则先删除旧表 (慎用，防止误删数据，适合初始化)
DROP TABLE IF EXISTS `invoice_record`;

-- 3. 创建 invoice_record 表
-- 注意：字段名对应 Java 实体类中的驼峰命名 (merchantName -> merchant_name)
CREATE TABLE `invoice_record` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `merchant_name` varchar(255) DEFAULT NULL COMMENT '商户名称',
                                  `item_name` varchar(255) DEFAULT NULL COMMENT '项目名称/商品明细',
                                  `invoice_code` varchar(50) DEFAULT NULL COMMENT '发票号码',
                                  `amount` double(10,2) DEFAULT NULL COMMENT '金额',
                                  `date` varchar(20) DEFAULT NULL COMMENT '开票日期 (存字符串方便)',
                                  `category` varchar(50) DEFAULT NULL COMMENT '智能分类',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能票据归档表';

CREATE TABLE `sys_user` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `username` varchar(50) NOT NULL COMMENT '用户名 (登录账号)',
                            `password` varchar(100) NOT NULL COMMENT '密码',
                            `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
                            `role` varchar(20) DEFAULT 'user' COMMENT '角色权限 (admin/user)',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`) USING BTREE COMMENT '用户名唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';