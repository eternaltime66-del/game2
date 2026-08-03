SET NAMES utf8mb4;

ALTER TABLE `app_game_finished_skill_effect`
  ADD COLUMN `use_weapon_ratio` tinyint NOT NULL DEFAULT 0 COMMENT '是否由武器释放(1=z读装备武器damage_ratio)' AFTER `ratio_y`;

-- 普攻、重击：由武器释放
UPDATE `app_game_finished_skill_effect`
SET `use_weapon_ratio` = 1, `ratio_z` = NULL
WHERE `id` IN ('fse_normal_atk_1', 'fse_heavy_1');

-- 非武器技能：不乘 z
UPDATE `app_game_finished_skill_effect`
SET `use_weapon_ratio` = 0, `ratio_z` = NULL
WHERE `id` = 'fse_regen_1';
