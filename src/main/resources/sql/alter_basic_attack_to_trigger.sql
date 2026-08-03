SET NAMES utf8mb4;

-- 将武器 basic_attack_finished_skill_id 迁移到扳机槽（ACTION_VALUE_FULL）
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

-- 可选：若列已存在则删除武器表上的冗余字段
-- ALTER TABLE `app_game_weapon` DROP COLUMN `basic_attack_finished_skill_id`;

-- 普攻技能不再单独分类，统一为普通主动技能
UPDATE `app_game_finished_skill`
SET `skill_category` = 'ACTIVE'
WHERE `skill_category` = 'BASIC_ATTACK';
