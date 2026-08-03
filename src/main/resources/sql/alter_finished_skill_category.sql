SET NAMES utf8mb4;

ALTER TABLE `app_game_finished_skill`
  ADD COLUMN `cat_l1` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT '分类1' AFTER `target_param`,
  ADD COLUMN `cat_l2` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT '分类2' AFTER `cat_l1`,
  ADD COLUMN `cat_l3` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT '分类3' AFTER `cat_l2`;

UPDATE `app_game_finished_skill`
SET `cat_l1` = 'GENERAL', `cat_l2` = 'GENERAL', `cat_l3` = 'GENERAL'
WHERE `cat_l1` IS NULL OR `cat_l1` = '';
