SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_weapon` (
  `id` varchar(32) NOT NULL COMMENT '武器ID',
  `item_id` varchar(32) NOT NULL COMMENT '关联物品ID',
  `attack` int NOT NULL DEFAULT 0 COMMENT '攻击力',
  `base_action_value` int NOT NULL DEFAULT 100 COMMENT '基础行动值',
  `damage_ratio` decimal(10,2) NOT NULL DEFAULT 1.00 COMMENT '伤害比例',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武器属性';

CREATE TABLE IF NOT EXISTS `app_game_armor` (
  `id` varchar(32) NOT NULL COMMENT '防具ID',
  `item_id` varchar(32) NOT NULL COMMENT '关联物品ID',
  `bonus_hp` int NOT NULL DEFAULT 0 COMMENT '生命加成',
  `defense` int NOT NULL DEFAULT 0 COMMENT '防御',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='防具属性';

INSERT INTO `app_game_weapon` (`id`, `item_id`, `attack`, `base_action_value`, `damage_ratio`, `enabled`, `remark`)
SELECT 'wpn_seven_hurt', 'item_seven_hurt', 25, 100, 1.00, 1, '七伤刀'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_weapon` WHERE `item_id` = 'item_seven_hurt');

INSERT INTO `app_game_armor` (`id`, `item_id`, `bonus_hp`, `defense`, `enabled`, `remark`)
SELECT 'arm_heal_armor', 'item_heal_armor', 50, 8, 1, '受伤回血甲'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_armor` WHERE `item_id` = 'item_heal_armor');
