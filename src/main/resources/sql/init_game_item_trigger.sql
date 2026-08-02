SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_item_trigger` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `item_id` varchar(32) NOT NULL COMMENT '绑定物品ID',
  `trigger_phase` varchar(32) NOT NULL COMMENT '扳机时机: BEFORE_ATTACK/AFTER_ATTACK/BEFORE_HIT/AFTER_HIT/BEFORE_TAKE_DAMAGE/AFTER_TAKE_DAMAGE',
  `effect_type` varchar(32) NOT NULL COMMENT '效果类型: DEAL_DAMAGE/TAKE_DAMAGE/HEAL',
  `effect_value` decimal(10,1) NOT NULL DEFAULT 0 COMMENT '效果数值',
  `sort` int NOT NULL DEFAULT 0 COMMENT '同物品内排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_phase` (`trigger_phase`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品扳机效果';

-- 七伤刀（特性武器示例）
INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `item_tags`, `max_stack`, `weight`, `sort`, `enabled`, `remark`)
SELECT 'item_seven_hurt_blade', 'SEVEN_HURT_BLADE', '七伤刀', '/img/items/seven_hurt_blade.png', 'WEAPON', 1, 3.5, 20, 1, '攻击前自损、攻击后追加伤害'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `id` = 'item_seven_hurt_blade');

INSERT INTO `app_game_weapon` (`id`, `item_id`, `attack`, `base_action_value`, `damage_ratio`, `enabled`, `remark`)
SELECT 'wpn_seven_hurt_blade', 'item_seven_hurt_blade', 18, 90, 1.10, 1, '七伤刀'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_weapon` WHERE `item_id` = 'item_seven_hurt_blade');

INSERT INTO `app_game_item_trigger` (`id`, `item_id`, `trigger_phase`, `effect_type`, `effect_value`, `sort`, `enabled`, `remark`)
SELECT 'trg_seven_hurt_pre', 'item_seven_hurt_blade', 'BEFORE_ATTACK', 'TAKE_DAMAGE', 3.0, 1, 1, '攻击前受到3点伤害'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item_trigger` WHERE `id` = 'trg_seven_hurt_pre');

INSERT INTO `app_game_item_trigger` (`id`, `item_id`, `trigger_phase`, `effect_type`, `effect_value`, `sort`, `enabled`, `remark`)
SELECT 'trg_seven_hurt_post', 'item_seven_hurt_blade', 'AFTER_ATTACK', 'DEAL_DAMAGE', 3.0, 2, 1, '攻击后造成3点伤害'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item_trigger` WHERE `id` = 'trg_seven_hurt_post');
