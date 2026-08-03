SET NAMES utf8mb4;

-- cat_l3: entry name (free text); cat_l4: slot type (GENERAL/BASIC_ATTACK/ULTIMATE/TRAIT_ACTIVE)
-- Run with: mysql --default-character-set=utf8mb4 -u root -p game2 < alter_finished_skill_cat_l4.sql

ALTER TABLE `app_game_finished_skill`
  MODIFY COLUMN `cat_l3` varchar(64) NOT NULL DEFAULT '通用' COMMENT 'cat3 entry name' AFTER `cat_l2`;

ALTER TABLE `app_game_finished_skill`
  ADD COLUMN IF NOT EXISTS `cat_l4` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT 'cat4 slot kind' AFTER `cat_l3`;

UPDATE `app_game_finished_skill`
SET `cat_l4` = `cat_l3`
WHERE `cat_l4` = 'GENERAL'
  AND `cat_l3` IN ('BASIC_ATTACK', 'ULTIMATE', 'TRAIT_ACTIVE');

UPDATE `app_game_finished_skill`
SET `cat_l3` = '通用'
WHERE `cat_l3` IN ('BASIC_ATTACK', 'ULTIMATE', 'TRAIT_ACTIVE', 'GENERAL');

UPDATE `app_game_finished_skill`
SET `cat_l1` = 'GENERAL', `cat_l2` = 'GENERAL', `cat_l3` = '通用', `cat_l4` = 'GENERAL'
WHERE (`cat_l1` IS NULL OR `cat_l1` = '')
   OR (`cat_l2` IS NULL OR `cat_l2` = '');
