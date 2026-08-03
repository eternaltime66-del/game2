SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_item` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `icon` varchar(64) NOT NULL DEFAULT '📦' COMMENT '图标',
  `weight` decimal(10,2) DEFAULT NULL COMMENT '重量',
  `item_tags` varchar(128) DEFAULT 'MATERIAL' COMMENT '标签,逗号分隔',
  `max_stack` int NOT NULL DEFAULT 99 COMMENT '最大堆叠',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏物品';

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `item_tags`, `max_stack`, `sort`, `enabled`, `remark`)
SELECT 'item_seven_hurt', 'SEVEN_HURT', '七伤刀', '🔪', 'WEAPON', 1, 10, 1, '攻击时额外造成伤害'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `id` = 'item_seven_hurt');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `item_tags`, `max_stack`, `sort`, `enabled`, `remark`)
SELECT 'item_heal_armor', 'HEAL_ARMOR', '受伤回血甲', '🛡', 'ARMOR', 1, 11, 1, '受伤时恢复生命'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `id` = 'item_heal_armor');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `item_tags`, `max_stack`, `sort`, `enabled`, `remark`)
SELECT 'item_leaf', 'LEAF', '新鲜树叶', '🍃', 'MATERIAL', 99, 1, 1, '基础材料'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `id` = 'item_leaf');
