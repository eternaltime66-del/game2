SET NAMES utf8mb4;

-- ========== 成品技能（组合高级效果 + 目标槽） ==========
CREATE TABLE IF NOT EXISTS `app_game_finished_skill` (
  `id` varchar(32) NOT NULL COMMENT '成品技能ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `target_type` varchar(32) NOT NULL COMMENT '目标槽类型',
  `target_param` int DEFAULT NULL COMMENT '目标参数(随机敌人数/重复次数等)',
  `cat_l1` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT '分类1',
  `cat_l2` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT '分类2',
  `cat_l3` varchar(64) NOT NULL DEFAULT '通用' COMMENT '分类3：入口名称',
  `cat_l4` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT '分类4：槽位类型',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成品技能';

CREATE TABLE IF NOT EXISTS `app_game_finished_skill_effect` (
  `id` varchar(32) NOT NULL,
  `finished_skill_id` varchar(32) NOT NULL,
  `effect_kind` varchar(32) NOT NULL COMMENT 'STAT_FORMULA/ACTION_VALUE/FIXED_VALUE',
  `outcome_type` varchar(16) NOT NULL COMMENT 'DAMAGE/HEAL',
  `stat_ref` varchar(16) DEFAULT NULL COMMENT 'ATTACK/DEFENSE/MAX_HP',
  `ratio_y` decimal(10,4) DEFAULT NULL COMMENT '比例y',
  `use_weapon_ratio` tinyint NOT NULL DEFAULT 0 COMMENT '是否由武器释放(1=z读装备武器damage_ratio)',
  `ratio_z` decimal(10,4) DEFAULT NULL COMMENT '已废弃，由use_weapon_ratio控制',
  `fixed_value` decimal(10,1) DEFAULT NULL COMMENT '固定值x',
  `action_delta` int DEFAULT NULL COMMENT '行动值增减',
  `sort` int NOT NULL DEFAULT 0,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_finished_skill_id` (`finished_skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成品技能-高级效果步骤';

-- ========== 完整技能组（扳机时机 → 释放成品技能） ==========
CREATE TABLE IF NOT EXISTS `app_game_complete_skill` (
  `id` varchar(32) NOT NULL,
  `code` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `trigger_slot_type` varchar(48) NOT NULL COMMENT '扳机槽类型',
  `trigger_param` decimal(10,1) DEFAULT NULL COMMENT '阈值x',
  `trigger_ref_id` varchar(32) DEFAULT NULL COMMENT '关联成品技能ID(释放次数类)',
  `finished_skill_id` varchar(32) NOT NULL COMMENT '触发的成品技能',
  `max_cast_count` int DEFAULT NULL COMMENT '单场最多释放次数，NULL=无限',
  `bind_type` varchar(16) NOT NULL DEFAULT 'DEFAULT' COMMENT 'DEFAULT/HERO/MONSTER/ITEM',
  `bind_ref_id` varchar(32) DEFAULT NULL COMMENT '绑定对象ID',
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_bind` (`bind_type`, `bind_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='完整技能组';

-- ========== 扳机槽（装备等物品上的额外扳机） ==========
CREATE TABLE IF NOT EXISTS `app_game_trigger_slot` (
  `id` varchar(32) NOT NULL,
  `item_id` varchar(32) DEFAULT NULL COMMENT '绑定物品,空=全局/角色',
  `monster_id` varchar(32) DEFAULT NULL COMMENT '绑定怪物',
  `slot_kind` varchar(32) NOT NULL DEFAULT 'TRAIT_ACTIVE' COMMENT 'BASIC_ATTACK/ULTIMATE/TRAIT_ACTIVE',
  `trigger_slot_type` varchar(48) NOT NULL,
  `trigger_param` decimal(10,1) DEFAULT NULL,
  `trigger_ref_id` varchar(32) DEFAULT NULL,
  `finished_skill_id` varchar(32) NOT NULL COMMENT '触发后释放的成品技能',
  `max_cast_count` int DEFAULT NULL COMMENT '单场最多释放次数，NULL=无限',
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扳机槽';

-- 普攻：前排随机1敌方，攻击力*100%伤害
INSERT INTO `app_game_finished_skill` (`id`, `code`, `name`, `target_type`, `target_param`, `enabled`, `remark`)
SELECT 'fin_normal_attack', 'NORMAL_ATTACK', '普攻', 'FRONT_ROW_RANDOM_ONE_ENEMY', NULL, 1, '行动值满：前排随机1敌方，攻击力100%×武器比例'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_finished_skill` WHERE `id` = 'fin_normal_attack');

INSERT INTO `app_game_finished_skill_effect`
(`id`, `finished_skill_id`, `effect_kind`, `outcome_type`, `stat_ref`, `ratio_y`, `use_weapon_ratio`, `sort`, `remark`)
SELECT 'fse_normal_atk_1', 'fin_normal_attack', 'STAT_FORMULA', 'DAMAGE', 'ATTACK', 1.0000, 1, 1, '攻击力*100%*武器伤害比例'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_finished_skill_effect` WHERE `id` = 'fse_normal_atk_1');

-- 完整技能：行动值满 → 普攻
INSERT INTO `app_game_complete_skill`
(`id`, `code`, `name`, `trigger_slot_type`, `trigger_param`, `finished_skill_id`, `bind_type`, `sort`, `enabled`, `remark`)
SELECT 'cmp_default_action_full', 'DEFAULT_ACTION_FULL', '基础普攻释放', 'ACTION_VALUE_FULL', NULL, 'fin_normal_attack', 'DEFAULT', 1, 1, '行动值满时释放普攻'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_complete_skill` WHERE `id` = 'cmp_default_action_full');

-- 重击：随机1敌方，攻击力*150%
INSERT INTO `app_game_finished_skill` (`id`, `code`, `name`, `target_type`, `target_param`, `enabled`, `remark`)
SELECT 'fin_heavy_strike', 'HEAVY_STRIKE', '重击', 'RANDOM_ONE_ENEMY', NULL, 1, '攻击力150%'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_finished_skill` WHERE `id` = 'fin_heavy_strike');

INSERT INTO `app_game_finished_skill_effect`
(`id`, `finished_skill_id`, `effect_kind`, `outcome_type`, `stat_ref`, `ratio_y`, `use_weapon_ratio`, `sort`, `remark`)
SELECT 'fse_heavy_1', 'fin_heavy_strike', 'STAT_FORMULA', 'DAMAGE', 'ATTACK', 1.5000, 1, 1, '攻击力*150%*武器伤害比例'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_finished_skill_effect` WHERE `id` = 'fse_heavy_1');

-- 自愈脉冲：自己，恢复最大生命10%
INSERT INTO `app_game_finished_skill` (`id`, `code`, `name`, `target_type`, `target_param`, `enabled`, `remark`)
SELECT 'fin_regen_pulse', 'REGEN_PULSE', '自愈脉冲', 'SELF', NULL, 1, '恢复10%最大生命'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_finished_skill` WHERE `id` = 'fin_regen_pulse');

INSERT INTO `app_game_finished_skill_effect`
(`id`, `finished_skill_id`, `effect_kind`, `outcome_type`, `stat_ref`, `ratio_y`, `use_weapon_ratio`, `sort`, `remark`)
SELECT 'fse_regen_1', 'fin_regen_pulse', 'STAT_FORMULA', 'HEAL', 'MAX_HP', 0.1000, 0, 1, '最大生命*10%'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_finished_skill_effect` WHERE `id` = 'fse_regen_1');

-- 七伤刀：每攻击时 → 重击
INSERT INTO `app_game_trigger_slot`
(`id`, `item_id`, `trigger_slot_type`, `trigger_param`, `finished_skill_id`, `sort`, `enabled`, `remark`)
SELECT 'ts_seven_on_attack', 'item_seven_hurt', 'ON_ATTACK', NULL, 'fin_heavy_strike', 1, 1, '攻击时追加重击'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_trigger_slot` WHERE `id` = 'ts_seven_on_attack');

-- 受伤回血甲：每受到伤害时 → 自愈脉冲
INSERT INTO `app_game_trigger_slot`
(`id`, `item_id`, `trigger_slot_type`, `trigger_param`, `finished_skill_id`, `sort`, `enabled`, `remark`)
SELECT 'ts_armor_on_hit', 'item_heal_armor', 'ON_TAKE_DAMAGE', NULL, 'fin_regen_pulse', 1, 1, '受伤时恢复生命'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_trigger_slot` WHERE `id` = 'ts_armor_on_hit');
