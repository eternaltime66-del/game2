-- 怪物难度分类 + 波次站位
SET NAMES utf8mb4;

-- 怪物：难度 / 体型（占地由难度推导，也可覆盖）
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `app_game_monster` ADD COLUMN `rank_type` varchar(16) NOT NULL DEFAULT ''NORMAL'' COMMENT ''NORMAL/ELITE/BOSS'' AFTER `action_value`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_monster' AND COLUMN_NAME = 'rank_type'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `app_game_monster` ADD COLUMN `footprint_w` int NOT NULL DEFAULT 1 COMMENT ''占地宽(列)'' AFTER `rank_type`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_monster' AND COLUMN_NAME = 'footprint_w'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `app_game_monster` ADD COLUMN `footprint_h` int NOT NULL DEFAULT 1 COMMENT ''占地高(行)'' AFTER `footprint_w`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_monster' AND COLUMN_NAME = 'footprint_h'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 波次怪物：可选固定站位（左上角格）
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `app_game_wave_monster` ADD COLUMN `slot_col` int DEFAULT NULL COMMENT ''站位列0-3'' AFTER `sort`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_wave_monster' AND COLUMN_NAME = 'slot_col'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `app_game_wave_monster` ADD COLUMN `slot_row` int DEFAULT NULL COMMENT ''站位行0-2,0=前排'' AFTER `slot_col`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_wave_monster' AND COLUMN_NAME = 'slot_row'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 种子：难度分类
UPDATE `app_game_monster` SET `rank_type` = 'NORMAL', `footprint_w` = 1, `footprint_h` = 1 WHERE `id` = 'mon_slime';
UPDATE `app_game_monster` SET `rank_type` = 'NORMAL', `footprint_w` = 1, `footprint_h` = 1 WHERE `id` = 'mon_goblin';
UPDATE `app_game_monster` SET `rank_type` = 'ELITE', `footprint_w` = 2, `footprint_h` = 1 WHERE `id` = 'mon_orc';
UPDATE `app_game_monster` SET `rank_type` = 'BOSS', `footprint_w` = 2, `footprint_h` = 2 WHERE `id` = 'mon_boss';
