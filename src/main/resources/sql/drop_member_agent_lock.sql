-- 移除 app_member 表中已废弃字段
ALTER TABLE app_member DROP COLUMN IF EXISTS agent;
ALTER TABLE app_member DROP COLUMN IF EXISTS `lock`;
