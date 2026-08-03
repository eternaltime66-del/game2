SET NAMES utf8mb4;

-- ========== 合并迁移：普攻/扳机槽/槽位分类 ==========

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_weapon'
    AND COLUMN_NAME = 'basic_attack_finished_skill_id'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `app_game_weapon` ADD COLUMN `basic_attack_finished_skill_id` varchar(32) DEFAULT NULL COMMENT ''普攻成品技能ID'' AFTER `damage_ratio`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_finished_skill'
    AND COLUMN_NAME = 'skill_category'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `app_game_finished_skill` ADD COLUMN `skill_category` varchar(32) NOT NULL DEFAULT ''ACTIVE'' AFTER `target_param`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `app_game_finished_skill`
SET `target_type` = 'FRONT_ROW_RANDOM_ONE_ENEMY',
    `remark` = '行动值满：前排随机1敌方，攻击力100%×武器比例'
WHERE `id` = 'fin_normal_attack';

UPDATE `app_game_finished_skill`
SET `skill_category` = 'ACTIVE'
WHERE `skill_category` = 'BASIC_ATTACK';

INSERT INTO `app_game_trigger_slot`
(`id`, `item_id`, `trigger_slot_type`, `trigger_param`, `trigger_ref_id`, `finished_skill_id`, `max_cast_count`, `sort`, `enabled`, `remark`)
SELECT CONCAT('ts_ba_', w.id), w.item_id, 'ACTION_VALUE_FULL', NULL, NULL, w.basic_attack_finished_skill_id, NULL, 0, 1, '武器普攻'
FROM `app_game_weapon` w
WHERE w.basic_attack_finished_skill_id IS NOT NULL
  AND w.basic_attack_finished_skill_id <> ''
  AND NOT EXISTS (
    SELECT 1 FROM `app_game_trigger_slot` ts
    WHERE ts.item_id = w.item_id AND ts.trigger_slot_type = 'ACTION_VALUE_FULL'
  );

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_trigger_slot'
    AND COLUMN_NAME = 'slot_kind'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `app_game_trigger_slot` ADD COLUMN `slot_kind` varchar(32) NOT NULL DEFAULT ''TRAIT_ACTIVE'' COMMENT ''BASIC_ATTACK/ULTIMATE/TRAIT_ACTIVE'' AFTER `item_id`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `app_game_trigger_slot`
SET `slot_kind` = 'BASIC_ATTACK'
WHERE `trigger_slot_type` = 'ACTION_VALUE_FULL'
  AND (`slot_kind` IS NULL OR `slot_kind` = '' OR `slot_kind` = 'TRAIT_ACTIVE');

UPDATE `app_game_trigger_slot`
SET `slot_kind` = 'TRAIT_ACTIVE'
WHERE `slot_kind` IS NULL OR `slot_kind` = '';
