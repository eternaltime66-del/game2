SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_skill` (
  `id` varchar(32) NOT NULL COMMENT '技能ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='完整技能';

CREATE TABLE IF NOT EXISTS `app_game_skill_effect` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `skill_id` varchar(32) NOT NULL COMMENT '技能ID',
  `effect_type` varchar(32) NOT NULL COMMENT 'DEAL_DAMAGE/TAKE_DAMAGE/HEAL',
  `effect_value` decimal(10,1) NOT NULL DEFAULT 0 COMMENT '效果数值',
  `target_type` varchar(32) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/ATTACK_TARGET/ATTACKER',
  `sort` int NOT NULL DEFAULT 0 COMMENT '执行顺序',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能效果步骤';

-- 扳机绑定完整技能（仅首次迁移执行）
-- ALTER TABLE `app_game_item_trigger`
--   ADD COLUMN `skill_id` varchar(32) DEFAULT NULL COMMENT '触发的完整技能ID' AFTER `item_id`,
--   ADD COLUMN `threshold_value` decimal(10,1) DEFAULT NULL COMMENT '累计类扳机阈值' AFTER `skill_id`;

-- 七伤刀技能
INSERT INTO `app_game_skill` (`id`, `code`, `name`, `sort`, `enabled`, `remark`)
SELECT 'sk_seven_hurt_pre', 'SEVEN_HURT_PRE', '七伤·攻前自损', 1, 1, '攻击前受到3点伤害'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_skill` WHERE `id` = 'sk_seven_hurt_pre');

INSERT INTO `app_game_skill` (`id`, `code`, `name`, `sort`, `enabled`, `remark`)
SELECT 'sk_seven_hurt_post', 'SEVEN_HURT_POST', '七伤·攻后追伤', 2, 1, '攻击后造成3点伤害'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_skill` WHERE `id` = 'sk_seven_hurt_post');

INSERT INTO `app_game_skill_effect` (`id`, `skill_id`, `effect_type`, `effect_value`, `target_type`, `sort`, `remark`)
SELECT 'ske_seven_hurt_pre_1', 'sk_seven_hurt_pre', 'TAKE_DAMAGE', 3.0, 'SELF', 1, '自损3'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_skill_effect` WHERE `id` = 'ske_seven_hurt_pre_1');

INSERT INTO `app_game_skill_effect` (`id`, `skill_id`, `effect_type`, `effect_value`, `target_type`, `sort`, `remark`)
SELECT 'ske_seven_hurt_post_1', 'sk_seven_hurt_post', 'DEAL_DAMAGE', 3.0, 'ATTACK_TARGET', 1, '追伤3'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_skill_effect` WHERE `id` = 'ske_seven_hurt_post_1');

UPDATE `app_game_item_trigger` SET `skill_id` = 'sk_seven_hurt_pre', `threshold_value` = NULL
WHERE `id` = 'trg_seven_hurt_pre' AND (`skill_id` IS NULL OR `skill_id` = '');

UPDATE `app_game_item_trigger` SET `skill_id` = 'sk_seven_hurt_post', `threshold_value` = NULL
WHERE `id` = 'trg_seven_hurt_post' AND (`skill_id` IS NULL OR `skill_id` = '');
