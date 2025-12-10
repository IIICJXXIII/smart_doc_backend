USE `smartdoc`;

-- 1. 增加 user_id 字段
ALTER TABLE `invoice_record` ADD COLUMN `user_id` BIGINT COMMENT '所属用户ID';

-- 2. (可选) 把现有的旧数据全部归属给管理员(id=1)，防止数据丢失或查不到
UPDATE `invoice_record` SET `user_id` = 1 WHERE `user_id` IS NULL;