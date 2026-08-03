SET NAMES utf8mb4;

ALTER TABLE `app_game_hero`
  ADD COLUMN `defense` int NOT NULL DEFAULT 0 COMMENT '基础防御' AFTER `attack`;

CREATE TABLE IF NOT EXISTS `app_game_armor` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `item_id` varchar(32) NOT NULL COMMENT '关联物品ID',
  `bonus_hp` int NOT NULL DEFAULT 0 COMMENT '生命加成',
  `defense` int NOT NULL DEFAULT 0 COMMENT '防御',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护甲属性';

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `item_tags`, `weight`, `max_stack`, `sort`, `enabled`, `remark`)
SELECT 'item_wood_armor', 'WOOD_ARMOR', '木甲', '/img/items/wood_armor.png', 'ARMOR', 1.2, 1, 20, 1, '简易木甲，提供基础防护'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'WOOD_ARMOR');

INSERT INTO `app_game_armor` (`id`, `item_id`, `bonus_hp`, `defense`, `enabled`, `remark`)
SELECT 'arm_wood', 'item_wood_armor', 160, 1, 1, '木甲'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_armor` WHERE `item_id` = 'item_wood_armor');

UPDATE `app_game_item`
SET `icon` = '/img/items/wood_armor.png', `item_tags` = 'ARMOR'
WHERE `id` = 'item_wood_armor';

UPDATE `app_game_hero` SET `defense` = 0 WHERE `defense` IS NULL;
