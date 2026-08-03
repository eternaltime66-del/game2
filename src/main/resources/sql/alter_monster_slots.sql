SET NAMES utf8mb4;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_trigger_slot'
    AND COLUMN_NAME = 'monster_id'
);
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `app_game_trigger_slot` ADD COLUMN `monster_id` varchar(32) DEFAULT NULL COMMENT ''绑定怪物'' AFTER `item_id`, ADD KEY `idx_monster_id` (`monster_id`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `app_game_monster_passive` (
  `id` varchar(32) NOT NULL,
  `monster_id` varchar(32) NOT NULL,
  `passive_skill_id` varchar(32) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_monster_id` (`monster_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='monster trait passive';
