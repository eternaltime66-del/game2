SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_item_passive` (
  `id` varchar(32) NOT NULL COMMENT '绑定ID',
  `item_id` varchar(32) NOT NULL COMMENT '装备物品ID',
  `passive_skill_id` varchar(32) NOT NULL COMMENT '被动技能ID',
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_passive_skill_id` (`passive_skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装备-被动技能绑定';

ALTER TABLE `app_game_armor`
  ADD COLUMN `bonus_attack` int NOT NULL DEFAULT 0 COMMENT '攻击加成（饰品等）' AFTER `defense`;
