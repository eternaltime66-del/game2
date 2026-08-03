SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_passive_skill` (
  `id` varchar(32) NOT NULL COMMENT '被动技能ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `condition_type` varchar(32) NOT NULL DEFAULT 'NONE' COMMENT '生效条件 NONE/REQUIRE_EQUIP',
  `condition_equip_item_id` varchar(32) DEFAULT NULL COMMENT '条件：需装备的物品ID',
  `effect_type` varchar(32) NOT NULL COMMENT '效果类型',
  `effect_value` decimal(10,4) NOT NULL DEFAULT 0 COMMENT '效果数值：固定值或百分比',
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='被动技能';

CREATE TABLE IF NOT EXISTS `app_game_skill_badge` (
  `item_id` varchar(32) NOT NULL COMMENT '技能徽章物品ID',
  `passive_skill_id` varchar(32) NOT NULL COMMENT '关联被动技能ID',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`item_id`),
  KEY `idx_passive_skill_id` (`passive_skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能徽章-被动关联';
