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

CREATE TABLE IF NOT EXISTS `app_game_craft_recipe` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `result_item_id` varchar(32) NOT NULL COMMENT '产物物品ID',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合成配方';

CREATE TABLE IF NOT EXISTS `app_game_craft_material` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `recipe_id` varchar(32) NOT NULL COMMENT '配方ID',
  `item_id` varchar(32) NOT NULL COMMENT '材料物品ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合成材料';

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `item_tags`, `weight`, `max_stack`, `sort`, `enabled`, `remark`)
SELECT 'item_wood_armor', 'WOOD_ARMOR', '木甲', '/img/items/wood_armor.png', 'ARMOR', 1.2, 1, 20, 1, '简易木甲，提供基础防护'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'WOOD_ARMOR');

INSERT INTO `app_game_armor` (`id`, `item_id`, `bonus_hp`, `defense`, `enabled`, `remark`)
SELECT 'arm_wood', 'item_wood_armor', 160, 1, 1, '木甲'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_armor` WHERE `item_id` = 'item_wood_armor');

INSERT INTO `app_game_craft_recipe` (`id`, `code`, `name`, `result_item_id`, `sort`, `enabled`, `remark`)
SELECT 'craft_wood_armor', 'WOOD_ARMOR', '木甲', 'item_wood_armor', 1, 1, '木棍×2 + 凝胶×4'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_craft_recipe` WHERE `code` = 'WOOD_ARMOR');

INSERT INTO `app_game_craft_material` (`id`, `recipe_id`, `item_id`, `quantity`, `sort`)
SELECT 'cm_wood_armor_stick', 'craft_wood_armor', 'item_stick', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_craft_material` WHERE `id` = 'cm_wood_armor_stick');

INSERT INTO `app_game_craft_material` (`id`, `recipe_id`, `item_id`, `quantity`, `sort`)
SELECT 'cm_wood_armor_gel', 'craft_wood_armor', 'item_slime_gel', 4, 2
WHERE NOT EXISTS (SELECT 1 FROM `app_game_craft_material` WHERE `id` = 'cm_wood_armor_gel');

UPDATE `app_game_item`
SET `icon` = '/img/items/wood_armor.png', `item_tags` = 'ARMOR'
WHERE `id` = 'item_wood_armor';

UPDATE `app_game_hero` SET `defense` = 0 WHERE `defense` IS NULL;
