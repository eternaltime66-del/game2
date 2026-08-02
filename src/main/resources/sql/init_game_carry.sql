SET NAMES utf8mb4;

ALTER TABLE `app_game_hero`
  ADD COLUMN `optimal_carry_weight` decimal(10,1) NOT NULL DEFAULT 10.0 COMMENT '最佳负重' AFTER `action_value`;

ALTER TABLE `app_game_item`
  ADD COLUMN `weight` decimal(10,1) NOT NULL DEFAULT 0.1 COMMENT '单件重量' AFTER `icon`;

CREATE TABLE IF NOT EXISTS `app_game_battle_bag` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `uid` varchar(32) NOT NULL COMMENT '用户ID',
  `item_id` varchar(32) NOT NULL COMMENT '物品ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid_item` (`uid`, `item_id`),
  KEY `idx_uid_sort` (`uid`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战斗背包';

UPDATE `app_game_item` SET `weight` = 0.1 WHERE `code` = 'LEAF';
UPDATE `app_game_item` SET `weight` = 0.3 WHERE `code` = 'STICK';
UPDATE `app_game_item` SET `weight` = 0.2 WHERE `code` = 'SLIME_GEL';
UPDATE `app_game_item` SET `weight` = 0.1 WHERE `code` = 'GOBLIN_TOOTH';
UPDATE `app_game_item` SET `weight` = 0.2 WHERE `code` = 'RAG';
UPDATE `app_game_item` SET `weight` = 0.1 WHERE `code` = 'COPPER_COIN';
UPDATE `app_game_item` SET `weight` = 0.5 WHERE `code` = 'ORC_BONE';
UPDATE `app_game_item` SET `weight` = 0.4 WHERE `code` = 'ROUGH_LEATHER';
UPDATE `app_game_item` SET `weight` = 1.0 WHERE `code` = 'IRON_ORE';
UPDATE `app_game_item` SET `weight` = 0.3 WHERE `code` = 'BOSS_BADGE';
UPDATE `app_game_item` SET `weight` = 0.5 WHERE `code` = 'RARE_CRYSTAL';
UPDATE `app_game_item` SET `weight` = 0.1 WHERE `code` = 'GOLD_COIN';

UPDATE `app_game_hero` SET `optimal_carry_weight` = 10.0 WHERE `optimal_carry_weight` IS NULL OR `optimal_carry_weight` <= 0;
