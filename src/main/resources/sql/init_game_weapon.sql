SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_weapon` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `item_id` varchar(32) NOT NULL COMMENT '关联物品ID',
  `attack` int NOT NULL DEFAULT 0 COMMENT '攻击力',
  `base_action_value` int NOT NULL DEFAULT 100 COMMENT '基础行动值(装备后替换空手)',
  `damage_ratio` decimal(10,2) NOT NULL DEFAULT 1.00 COMMENT '单次攻击伤害比例',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武器属性';

CREATE TABLE IF NOT EXISTS `app_game_hero_equip` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `uid` varchar(32) NOT NULL COMMENT '用户ID',
  `weapon_item_id` varchar(32) DEFAULT NULL COMMENT '武器',
  `armor_item_id` varchar(32) DEFAULT NULL COMMENT '护甲',
  `gloves_item_id` varchar(32) DEFAULT NULL COMMENT '护手',
  `legs_item_id` varchar(32) DEFAULT NULL COMMENT '护腿',
  `helmet_item_id` varchar(32) DEFAULT NULL COMMENT '头盔',
  `accessory1_item_id` varchar(32) DEFAULT NULL COMMENT '饰品1',
  `accessory2_item_id` varchar(32) DEFAULT NULL COMMENT '饰品2',
  `accessory3_item_id` varchar(32) DEFAULT NULL COMMENT '饰品3',
  `skill_badge1_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章1',
  `skill_badge2_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章2',
  `skill_badge3_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章3',
  `skill_badge4_item_id` varchar(32) DEFAULT NULL COMMENT '技能徽章4',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色装备';

INSERT INTO `app_game_weapon` (`id`, `item_id`, `attack`, `base_action_value`, `damage_ratio`, `enabled`, `remark`)
SELECT 'wpn_stick', 'item_stick', 12, 85, 1.20, 1, '粗木棍'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_weapon` WHERE `item_id` = 'item_stick');
