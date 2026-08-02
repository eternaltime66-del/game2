SET NAMES utf8mb4;

ALTER TABLE `app_game_item_trigger`
  ADD COLUMN `skill_id` varchar(32) DEFAULT NULL COMMENT '触发的完整技能ID' AFTER `item_id`;

ALTER TABLE `app_game_item_trigger`
  ADD COLUMN `threshold_value` decimal(10,1) DEFAULT NULL COMMENT '累计类扳机阈值' AFTER `skill_id`;
