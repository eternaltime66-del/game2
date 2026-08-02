SET NAMES utf8mb4;

ALTER TABLE `app_game_item`
  ADD COLUMN `item_tags` varchar(128) NOT NULL DEFAULT 'MATERIAL' COMMENT '多标签逗号分隔' AFTER `weight`;

UPDATE `app_game_item` SET `item_tags` = `item_tag` WHERE `item_tag` IS NOT NULL AND `item_tag` <> '';

UPDATE `app_game_item` SET `item_tags` = 'MATERIAL,WEAPON' WHERE `code` = 'STICK';

UPDATE `app_game_item` SET `item_tags` = 'MATERIAL'
WHERE `code` IN (
  'LEAF', 'SLIME_GEL', 'GOBLIN_TOOTH', 'RAG', 'COPPER_COIN',
  'ORC_BONE', 'ROUGH_LEATHER', 'IRON_ORE', 'BOSS_BADGE', 'RARE_CRYSTAL', 'GOLD_COIN'
);

ALTER TABLE `app_game_item` DROP COLUMN `item_tag`;
