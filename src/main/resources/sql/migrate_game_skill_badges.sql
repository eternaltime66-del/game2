SET NAMES utf8mb4;

ALTER TABLE `app_game_hero_equip`
  ADD COLUMN `skill_badge1_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章1' AFTER `accessory3_item_id`,
  ADD COLUMN `skill_badge2_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章2' AFTER `skill_badge1_item_id`,
  ADD COLUMN `skill_badge3_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章3' AFTER `skill_badge2_item_id`,
  ADD COLUMN `skill_badge4_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章4' AFTER `skill_badge3_item_id`;

UPDATE `app_game_item`
SET `item_tags` = REPLACE(`item_tags`, 'PROFESSION_BADGE', 'SKILL_BADGE')
WHERE `item_tags` LIKE '%PROFESSION_BADGE%';

ALTER TABLE `app_game_hero_equip`
  DROP COLUMN `profession_badge_item_id`;
