SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_monster_drop` (
  `id` varchar(32) NOT NULL,
  `monster_id` varchar(32) NOT NULL COMMENT '怪物ID',
  `item_id` varchar(32) NOT NULL COMMENT '掉落物品ID',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_monster_id` (`monster_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='怪物掉落';

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `enabled`)
SELECT 'md_slime_gel', 'mon_slime', 'item_slime_gel', 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'md_slime_gel');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `enabled`)
SELECT 'md_slime_leaf', 'mon_slime', 'item_leaf', 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'md_slime_leaf');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `enabled`)
SELECT 'md_goblin_tooth', 'mon_goblin', 'item_goblin_tooth', 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'md_goblin_tooth');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `enabled`)
SELECT 'md_orc_bone', 'mon_orc', 'item_orc_bone', 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'md_orc_bone');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `enabled`)
SELECT 'md_boss_badge', 'mon_boss', 'item_boss_badge', 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'md_boss_badge');

INSERT INTO `app_game_monster_drop` (`id`, `monster_id`, `item_id`, `enabled`)
SELECT 'md_boss_crystal', 'mon_boss', 'item_rare_crystal', 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_monster_drop` WHERE `id` = 'md_boss_crystal');
