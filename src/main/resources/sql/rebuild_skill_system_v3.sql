SET NAMES utf8mb4;

-- ========== 清空旧技能数据（含绑定） ==========
DELETE FROM `app_game_finished_skill_effect`;
DELETE FROM `app_game_trigger_slot`;
DELETE FROM `app_game_complete_skill`;
DELETE FROM `app_game_item_passive`;
DELETE FROM `app_game_monster_passive`;
DELETE FROM `app_game_skill_badge`;
DELETE FROM `app_game_passive_skill`;
DELETE FROM `app_game_finished_skill`;

-- ========== 成品/扳机技能新字段 ==========
SET @db := DATABASE();

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_finished_skill` ADD COLUMN `hit_frequency` int NOT NULL DEFAULT 1 COMMENT ''频率槽'' AFTER `target_param`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_finished_skill' AND COLUMN_NAME = 'hit_frequency'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_finished_skill` ADD COLUMN `max_cast_count` int DEFAULT NULL COMMENT ''全场最多发动次数 NULL=无限'' AFTER `hit_frequency`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_finished_skill' AND COLUMN_NAME = 'max_cast_count'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_finished_skill` ADD COLUMN `formulas_json` mediumtext COMMENT ''公式组JSON'' AFTER `max_cast_count`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_finished_skill' AND COLUMN_NAME = 'formulas_json'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ========== 扳机槽新字段 ==========
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_trigger_slot` ADD COLUMN `trigger_mode` varchar(16) NOT NULL DEFAULT ''PRECISE'' COMMENT ''PRECISE/QUICK'' AFTER `slot_kind`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_trigger_slot' AND COLUMN_NAME = 'trigger_mode'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_trigger_slot` ADD COLUMN `quick_preset` varchar(48) DEFAULT NULL COMMENT ''快捷扳机预设'' AFTER `trigger_mode`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_trigger_slot' AND COLUMN_NAME = 'quick_preset'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_trigger_slot` ADD COLUMN `conditions_json` mediumtext COMMENT ''条件组JSON'' AFTER `quick_preset`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_trigger_slot' AND COLUMN_NAME = 'conditions_json'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE `app_game_trigger_slot`
  MODIFY COLUMN `trigger_slot_type` varchar(48) DEFAULT NULL,
  MODIFY COLUMN `finished_skill_id` varchar(32) DEFAULT NULL;

-- ========== 被动技能新字段 ==========
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_passive_skill` ADD COLUMN `conditions_json` mediumtext COMMENT ''生效条件组JSON'' AFTER `owner_ref`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_passive_skill' AND COLUMN_NAME = 'conditions_json'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_passive_skill` ADD COLUMN `effects_json` mediumtext COMMENT ''效果组JSON'' AFTER `conditions_json`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'app_game_passive_skill' AND COLUMN_NAME = 'effects_json'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

ALTER TABLE `app_game_passive_skill`
  MODIFY COLUMN `condition_type` varchar(32) DEFAULT NULL,
  MODIFY COLUMN `effect_type` varchar(32) DEFAULT NULL,
  MODIFY COLUMN `effect_value` decimal(12,4) DEFAULT NULL;

-- ========== 通用普攻 ==========
INSERT INTO `app_game_finished_skill`
(`id`, `code`, `name`, `target_type`, `target_param`, `hit_frequency`, `max_cast_count`, `formulas_json`,
 `cat_l1`, `cat_l2`, `cat_l3`, `cat_l4`, `enabled`, `remark`)
VALUES (
  'fin_normal_attack',
  'NORMAL_ATTACK',
  CAST(UNHEX('E699AEE9809AE694BBE587BB') AS CHAR CHARACTER SET utf8mb4),
  'FIRST_TARGET',
  NULL,
  1,
  NULL,
  '[{"outcome":"DAMAGE","tokens":[{"kind":"READ","read":"CHAR_ATTACK"},{"kind":"OP","op":"*"},{"kind":"CONST","value":1}]}]',
  'PERSON',
  'GENERAL',
  CAST(UNHEX('E699AEE694BB') AS CHAR CHARACTER SET utf8mb4),
  'BASIC_ATTACK',
  1,
  CAST(UNHEX('E883BDE9878FE580BCE6BBA1E697B6EFBC9AE9A696E4BD8DE79BAEE6A087EFBC8CE694BBE587BBE58A9B31303025') AS CHAR CHARACTER SET utf8mb4)
);
