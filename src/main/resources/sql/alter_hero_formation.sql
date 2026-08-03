-- 主角布阵站位
SET NAMES utf8mb4;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `app_game_hero` ADD COLUMN `slot_col` int NOT NULL DEFAULT 1 COMMENT ''布阵列0-3(主角占2格)'' AFTER `action_value`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_hero' AND COLUMN_NAME = 'slot_col'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `app_game_hero` ADD COLUMN `slot_row` int NOT NULL DEFAULT 0 COMMENT ''布阵行0-2(0=前排)'' AFTER `slot_col`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_hero' AND COLUMN_NAME = 'slot_row'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `app_game_hero` SET `slot_col` = 1, `slot_row` = 0
WHERE `slot_col` IS NULL OR `slot_row` IS NULL;
