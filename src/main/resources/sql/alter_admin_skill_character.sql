-- 技能分类扩展 / 被动类型 / 角色模板 / 职业 / 消耗型武器
SET NAMES utf8mb4;

-- 被动：数值型 / 机制型
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_passive_skill` ADD COLUMN `passive_kind` varchar(16) NOT NULL DEFAULT ''NUMERIC'' COMMENT ''NUMERIC/MECHANISM'' AFTER `name`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_passive_skill' AND COLUMN_NAME = 'passive_kind'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `app_game_passive_skill` SET `passive_kind` = 'NUMERIC' WHERE `passive_kind` IS NULL OR `passive_kind` = '';

-- 武器消耗型
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_weapon` ADD COLUMN `consumable` tinyint NOT NULL DEFAULT 0 COMMENT ''是否消耗型'' AFTER `damage_ratio`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_weapon' AND COLUMN_NAME = 'consumable'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_weapon` ADD COLUMN `max_uses` int DEFAULT NULL COMMENT ''最大使用次数'' AFTER `consumable`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_weapon' AND COLUMN_NAME = 'max_uses'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_hero_equip` ADD COLUMN `weapon_uses_left` int DEFAULT NULL COMMENT ''消耗型武器剩余次数'' AFTER `weapon_item_id`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_hero_equip' AND COLUMN_NAME = 'weapon_uses_left'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 角色模板（后台改基础数值 → 登录同步）
CREATE TABLE IF NOT EXISTS `app_game_character_template` (
  `id` varchar(32) NOT NULL,
  `code` varchar(32) NOT NULL COMMENT 'PROTAGONIST等',
  `name` varchar(64) NOT NULL,
  `max_hp` int NOT NULL DEFAULT 200,
  `attack` int NOT NULL DEFAULT 10,
  `defense` int NOT NULL DEFAULT 0,
  `action_value` int NOT NULL DEFAULT 100,
  `template_version` int NOT NULL DEFAULT 1,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家角色模板';

INSERT INTO `app_game_character_template`
(`id`,`code`,`name`,`max_hp`,`attack`,`defense`,`action_value`,`template_version`,`enabled`,`remark`)
SELECT 'char_tpl_protagonist','PROTAGONIST',CAST(UNHEX('E4B8BBE8A792') AS CHAR CHARACTER SET utf8mb4),200,10,0,100,1,1,CAST(UNHEX('E9BB98E8AEA4E4B8BBE8A792E6A8A1E69DBF') AS CHAR CHARACTER SET utf8mb4)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_character_template` WHERE `code` = 'PROTAGONIST');

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_hero` ADD COLUMN `template_code` varchar(32) NOT NULL DEFAULT ''PROTAGONIST'' AFTER `name`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_hero' AND COLUMN_NAME = 'template_code'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `app_game_hero` ADD COLUMN `template_version` int NOT NULL DEFAULT 0 COMMENT ''已同步的模板版本'' AFTER `template_code`',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_game_hero' AND COLUMN_NAME = 'template_version'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 职业
CREATE TABLE IF NOT EXISTS `app_game_profession` (
  `id` varchar(32) NOT NULL,
  `code` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职业';

CREATE TABLE IF NOT EXISTS `app_game_profession_skill` (
  `id` varchar(32) NOT NULL,
  `profession_id` varchar(32) NOT NULL,
  `item_id` varchar(32) NOT NULL COMMENT '技能物品(可进技能槽)',
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prof_item` (`profession_id`,`item_id`),
  KEY `idx_profession` (`profession_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职业技能物品绑定';
