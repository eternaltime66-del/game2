-- 物品定义
CREATE TABLE IF NOT EXISTS `app_game_item` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `icon` varchar(64) NOT NULL DEFAULT '📦' COMMENT '图标',
  `max_stack` int NOT NULL DEFAULT 99 COMMENT '最大堆叠',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏物品';

-- 怪物掉落配置
CREATE TABLE IF NOT EXISTS `app_game_monster_drop` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `monster_id` varchar(32) NOT NULL COMMENT '怪物ID',
  `item_id` varchar(32) NOT NULL COMMENT '物品ID',
  `drop_rate` int NOT NULL DEFAULT 100 COMMENT '掉落概率0-100',
  `min_qty` int NOT NULL DEFAULT 1 COMMENT '最小数量',
  `max_qty` int NOT NULL DEFAULT 1 COMMENT '最大数量',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_monster_id` (`monster_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='怪物掉落配置';

-- 玩家仓库容量
CREATE TABLE IF NOT EXISTS `app_game_warehouse` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `uid` varchar(32) NOT NULL COMMENT '用户ID',
  `max_slots` int NOT NULL DEFAULT 100 COMMENT '最大格数',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家仓库';

-- 仓库格子（有物品的格才存行）
CREATE TABLE IF NOT EXISTS `app_game_inventory` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `uid` varchar(32) NOT NULL COMMENT '用户ID',
  `slot_no` int NOT NULL COMMENT '格子序号1-based',
  `item_id` varchar(32) NOT NULL COMMENT '物品ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid_slot` (`uid`, `slot_no`),
  KEY `idx_uid_item` (`uid`, `item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库物品';

-- 物品变动日志
CREATE TABLE IF NOT EXISTS `app_game_item_log` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `uid` varchar(32) NOT NULL COMMENT '用户ID',
  `item_id` varchar(32) NOT NULL COMMENT '物品ID',
  `item_name` varchar(64) NOT NULL COMMENT '物品名称',
  `change_qty` int NOT NULL COMMENT '变动数量(正增负减)',
  `before_qty` int NOT NULL DEFAULT 0 COMMENT '变动前数量',
  `after_qty` int NOT NULL DEFAULT 0 COMMENT '变动后数量',
  `reason` varchar(32) NOT NULL COMMENT '原因',
  `ref_id` varchar(64) DEFAULT NULL COMMENT '关联ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_uid_time` (`uid`, `CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品变动日志';

-- 物品种子
INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_leaf', 'LEAF', '新鲜树叶', '/img/items/leaf.png', 99, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'LEAF');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_stick', 'STICK', '粗木棍', '/img/items/stick.png', 99, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'STICK');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_slime_gel', 'SLIME_GEL', '史莱姆凝胶', '/img/items/slime_gel.png', 50, 3, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'SLIME_GEL');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_goblin_tooth', 'GOBLIN_TOOTH', '哥布林獠牙', '/img/items/goblin_tooth.png', 50, 4, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'GOBLIN_TOOTH');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_rag', 'RAG', '破旧布料', '/img/items/rag.png', 99, 5, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'RAG');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_copper_coin', 'COPPER_COIN', '铜币', '/img/items/copper_coin.png', 999, 6, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'COPPER_COIN');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_orc_bone', 'ORC_BONE', '兽人骨片', '/img/items/orc_bone.png', 50, 7, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'ORC_BONE');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_rough_leather', 'ROUGH_LEATHER', '粗制皮革', '/img/items/rough_leather.png', 50, 8, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'ROUGH_LEATHER');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_iron_ore', 'IRON_ORE', '铁矿石', '/img/items/iron_ore.png', 30, 9, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'IRON_ORE');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_boss_badge', 'BOSS_BADGE', '头目徽章', '/img/items/boss_badge.png', 10, 10, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'BOSS_BADGE');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_rare_crystal', 'RARE_CRYSTAL', '稀有水晶', '/img/items/rare_crystal.png', 20, 11, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'RARE_CRYSTAL');

INSERT INTO `app_game_item` (`id`, `code`, `name`, `icon`, `max_stack`, `sort`, `enabled`)
SELECT 'item_gold_coin', 'GOLD_COIN', '金币', '/img/items/gold_coin.png', 999, 12, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_item` WHERE `code` = 'GOLD_COIN');

-- 史莱姆掉落
INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_slime_leaf', 'mon_slime', 'item_leaf', 70, 1, 3, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_slime_leaf');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_slime_stick', 'mon_slime', 'item_stick', 60, 1, 2, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_slime_stick');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_slime_gel', 'mon_slime', 'item_slime_gel', 100, 1, 2, 3, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_slime_gel');

-- 哥布林掉落
INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_goblin_tooth', 'mon_goblin', 'item_goblin_tooth', 50, 1, 2, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_goblin_tooth');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_goblin_rag', 'mon_goblin', 'item_rag', 80, 1, 3, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_goblin_rag');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_goblin_coin', 'mon_goblin', 'item_copper_coin', 40, 1, 5, 3, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_goblin_coin');

-- 兽人掉落
INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_orc_bone', 'mon_orc', 'item_orc_bone', 70, 1, 2, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_orc_bone');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_orc_leather', 'mon_orc', 'item_rough_leather', 55, 1, 2, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_orc_leather');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_orc_iron', 'mon_orc', 'item_iron_ore', 35, 1, 1, 3, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_orc_iron');

-- 关底头目掉落
INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_boss_badge', 'mon_boss', 'item_boss_badge', 100, 1, 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_boss_badge');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_boss_crystal', 'mon_boss', 'item_rare_crystal', 60, 1, 2, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_boss_crystal');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `drop_rate`, `min_qty`, `max_qty`, `sort`, `enabled`)
SELECT 'drop_boss_gold', 'mon_boss', 'item_gold_coin', 80, 2, 5, 3, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'drop_boss_gold');
