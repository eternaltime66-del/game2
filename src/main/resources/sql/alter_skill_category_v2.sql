-- 被动技能增加装备/人物分类维度（复用 cat 语义，不动成品技能主表）
SET NAMES utf8mb4;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_passive_skill` ADD COLUMN `cat_l1` varchar(16) NOT NULL DEFAULT ''EQUIP'' COMMENT ''EQUIP/PERSON'' AFTER `passive_kind`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_passive_skill' AND COLUMN_NAME = 'cat_l1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_passive_skill` ADD COLUMN `cat_l2` varchar(16) NOT NULL DEFAULT ''GENERAL'' COMMENT ''装备部位或角色/职业/通用'' AFTER `cat_l1`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_passive_skill' AND COLUMN_NAME = 'cat_l2'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_passive_skill` ADD COLUMN `owner_ref` varchar(64) DEFAULT NULL COMMENT ''角色模板code或职业id'' AFTER `cat_l2`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_passive_skill' AND COLUMN_NAME = 'owner_ref'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `app_game_passive_skill` SET `cat_l1` = 'EQUIP' WHERE `cat_l1` IS NULL OR `cat_l1` = '';
UPDATE `app_game_passive_skill` SET `cat_l2` = 'GENERAL' WHERE `cat_l2` IS NULL OR `cat_l2` = '';
