SET NAMES utf8mb4;

ALTER TABLE `app_game_hero_equip`
  ADD COLUMN `armor_item_id` varchar(32) DEFAULT NULL COMMENT '护甲' AFTER `weapon_item_id`,
  ADD COLUMN `gloves_item_id` varchar(32) DEFAULT NULL COMMENT '护手' AFTER `armor_item_id`,
  ADD COLUMN `legs_item_id` varchar(32) DEFAULT NULL COMMENT '护腿' AFTER `gloves_item_id`,
  ADD COLUMN `helmet_item_id` varchar(32) DEFAULT NULL COMMENT '头盔' AFTER `legs_item_id`,
  ADD COLUMN `accessory1_item_id` varchar(32) DEFAULT NULL COMMENT '饰品1' AFTER `helmet_item_id`,
  ADD COLUMN `accessory2_item_id` varchar(32) DEFAULT NULL COMMENT '饰品2' AFTER `accessory1_item_id`,
  ADD COLUMN `accessory3_item_id` varchar(32) DEFAULT NULL COMMENT '饰品3' AFTER `accessory2_item_id`,
  ADD COLUMN `profession_badge_item_id` varchar(32) DEFAULT NULL COMMENT '职业徽章' AFTER `accessory3_item_id`;
