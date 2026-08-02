CREATE TABLE IF NOT EXISTS `app_game_mode_group` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大关卡模式分组';

CREATE TABLE IF NOT EXISTS `app_game_chapter` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `group_id` varchar(32) NOT NULL COMMENT '模式分组ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大关卡';

CREATE TABLE IF NOT EXISTS `app_game_stage_group` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `chapter_id` varchar(32) NOT NULL COMMENT '大关卡ID',
  `group_no` int NOT NULL COMMENT '组编号(1,2,3)',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chapter_group_no` (`chapter_id`, `group_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小关卡组';

CREATE TABLE IF NOT EXISTS `app_game_stage` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `stage_group_id` varchar(32) NOT NULL COMMENT '小关卡组ID',
  `stage_no` int NOT NULL COMMENT '关内序号(1,2,3)',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_stage_no` (`stage_group_id`, `stage_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小关卡';

INSERT INTO `app_game_mode_group` (`id`, `code`, `name`, `sort`, `enabled`, `remark`)
SELECT 'mode_core', 'CORE', '核心模式', 1, 1, 'core mode'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_mode_group` WHERE `code` = 'CORE');

INSERT INTO `app_game_chapter` (`id`, `group_id`, `code`, `name`, `sort`, `enabled`, `remark`)
SELECT 'chapter_main', 'mode_core', 'MAIN', '主线', 1, 1, 'main chapter'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_chapter` WHERE `code` = 'MAIN');

INSERT INTO `app_game_stage_group` (`id`, `chapter_id`, `group_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'sg_1', 'chapter_main', 1, '第1组', 1, 1, 'group 1'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage_group` WHERE `id` = 'sg_1');

INSERT INTO `app_game_stage_group` (`id`, `chapter_id`, `group_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'sg_2', 'chapter_main', 2, '第2组', 2, 1, 'group 2'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage_group` WHERE `id` = 'sg_2');

INSERT INTO `app_game_stage` (`id`, `stage_group_id`, `stage_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'st_1_1', 'sg_1', 1, '1-1 初入江湖', 1, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage` WHERE `id` = 'st_1_1');

INSERT INTO `app_game_stage` (`id`, `stage_group_id`, `stage_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'st_1_2', 'sg_1', 2, '1-2 小试牛刀', 2, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage` WHERE `id` = 'st_1_2');

INSERT INTO `app_game_stage` (`id`, `stage_group_id`, `stage_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'st_2_1', 'sg_2', 1, '2-1 再战强敌', 1, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage` WHERE `id` = 'st_2_1');

INSERT INTO `app_game_stage` (`id`, `stage_group_id`, `stage_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'st_2_2', 'sg_2', 2, '2-2 险境求生', 2, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage` WHERE `id` = 'st_2_2');

INSERT INTO `app_game_stage` (`id`, `stage_group_id`, `stage_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'st_2_3', 'sg_2', 3, '2-3 组末试炼', 3, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage` WHERE `id` = 'st_2_3');
