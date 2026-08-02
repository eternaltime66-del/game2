SET NAMES utf8mb4;

ALTER TABLE `app_game_item`
  ADD COLUMN `item_tag` varchar(16) NOT NULL DEFAULT 'MATERIAL' COMMENT '物品标签 MATERIAL材料 WEAPON武器' AFTER `weight`;

UPDATE `app_game_item` SET `item_tag` = 'WEAPON' WHERE `code` = 'STICK';
UPDATE `app_game_item` SET `item_tag` = 'MATERIAL' WHERE `item_tag` IS NULL OR `item_tag` = '';

ALTER TABLE `app_game_item` DROP COLUMN `category`;
ALTER TABLE `app_game_item` DROP COLUMN `sub_tags`;
