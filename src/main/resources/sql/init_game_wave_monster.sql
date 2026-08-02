CREATE TABLE IF NOT EXISTS `app_game_monster` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `hp` int NOT NULL DEFAULT 100 COMMENT '当前生命模板',
  `max_hp` int NOT NULL DEFAULT 100 COMMENT '最大生命',
  `attack` int NOT NULL DEFAULT 10 COMMENT '攻击力',
  `action_value` int NOT NULL DEFAULT 100 COMMENT '行动值',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='怪物';

CREATE TABLE IF NOT EXISTS `app_game_wave` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `stage_id` varchar(32) NOT NULL COMMENT '小关卡ID',
  `wave_no` int NOT NULL COMMENT '波次序号',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_wave_no` (`stage_id`, `wave_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小关卡波次';

CREATE TABLE IF NOT EXISTS `app_game_wave_monster` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `wave_id` varchar(32) NOT NULL COMMENT '波次ID',
  `monster_id` varchar(32) NOT NULL COMMENT '怪物ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wave_id` (`wave_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='波次怪物';

INSERT INTO `app_game_monster` (`id`, `code`, `name`, `hp`, `max_hp`, `attack`, `action_value`, `sort`, `enabled`)
SELECT 'mon_slime', 'SLIME', '史莱姆', 60, 60, 6, 90, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster` WHERE `code` = 'SLIME');

INSERT INTO `app_game_monster` (`id`, `code`, `name`, `hp`, `max_hp`, `attack`, `action_value`, `sort`, `enabled`)
SELECT 'mon_goblin', 'GOBLIN', '哥布林', 90, 90, 9, 95, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster` WHERE `code` = 'GOBLIN');

INSERT INTO `app_game_monster` (`id`, `code`, `name`, `hp`, `max_hp`, `attack`, `action_value`, `sort`, `enabled`)
SELECT 'mon_orc', 'ORC', '兽人', 140, 140, 14, 80, 3, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster` WHERE `code` = 'ORC');

INSERT INTO `app_game_monster` (`id`, `code`, `name`, `hp`, `max_hp`, `attack`, `action_value`, `sort`, `enabled`)
SELECT 'mon_boss', 'BOSS', '关底头目', 260, 260, 18, 100, 4, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster` WHERE `code` = 'BOSS');

INSERT INTO `app_game_wave` (`id`, `stage_id`, `wave_no`, `name`, `sort`, `enabled`)
SELECT 'wave_1_1_1', 'st_1_1', 1, '第1波', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave` WHERE `id` = 'wave_1_1_1');

INSERT INTO `app_game_wave` (`id`, `stage_id`, `wave_no`, `name`, `sort`, `enabled`)
SELECT 'wave_1_2_1', 'st_1_2', 1, '第1波', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave` WHERE `id` = 'wave_1_2_1');

INSERT INTO `app_game_wave` (`id`, `stage_id`, `wave_no`, `name`, `sort`, `enabled`)
SELECT 'wave_1_3_1', 'st_1_3', 1, '第1波', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave` WHERE `id` = 'wave_1_3_1');

INSERT INTO `app_game_wave` (`id`, `stage_id`, `wave_no`, `name`, `sort`, `enabled`)
SELECT 'wave_1_4_1', 'st_1_4', 1, '第1波', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave` WHERE `id` = 'wave_1_4_1');

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_1_1', 'wave_1_1_1', 'mon_slime', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_1_1');

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_2_1_1', 'wave_1_2_1', 'mon_goblin', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_2_1_1');

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_2_1_2', 'wave_1_2_1', 'mon_slime', 1, 2
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_2_1_2');

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_3_1_1', 'wave_1_3_1', 'mon_goblin', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_3_1_1');

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_4_1_1', 'wave_1_4_1', 'mon_orc', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_4_1_1');
