SET NAMES utf8mb4;

-- 第1组新增 1-3、1-4 小关
INSERT INTO `app_game_stage` (`id`, `stage_group_id`, `stage_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'st_1_3', 'sg_1', 3, '1-3 哥布林巢穴', 3, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage` WHERE `id` = 'st_1_3');

INSERT INTO `app_game_stage` (`id`, `stage_group_id`, `stage_no`, `name`, `sort`, `enabled`, `remark`)
SELECT 'st_1_4', 'sg_1', 4, '1-4 兽人首领', 4, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage` WHERE `id` = 'st_1_4');

-- 第1组每关仅保留 1 波：删除 1-2 多余波次
DELETE FROM `app_game_wave_monster`
WHERE `wave_id` IN ('wave_1_2_2', 'wave_1_2_3');

DELETE FROM `app_game_wave`
WHERE `id` IN ('wave_1_2_2', 'wave_1_2_3');

-- 1-2：1 哥布林 + 1 史莱姆
UPDATE `app_game_wave_monster`
SET `monster_id` = 'mon_goblin', `quantity` = 1, `sort` = 1
WHERE `id` = 'wm_1_2_1_1';

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_2_1_2', 'wave_1_2_1', 'mon_slime', 1, 2
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_2_1_2');

-- 1-3：2 哥布林
INSERT INTO `app_game_wave` (`id`, `stage_id`, `wave_no`, `name`, `sort`, `enabled`)
SELECT 'wave_1_3_1', 'st_1_3', 1, '第1波', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave` WHERE `id` = 'wave_1_3_1');

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_3_1_1', 'wave_1_3_1', 'mon_goblin', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_3_1_1');

-- 1-4：1 只 Boss 兽人
INSERT INTO `app_game_wave` (`id`, `stage_id`, `wave_no`, `name`, `sort`, `enabled`)
SELECT 'wave_1_4_1', 'st_1_4', 1, '第1波', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave` WHERE `id` = 'wave_1_4_1');

INSERT INTO `app_game_wave_monster` (`id`, `wave_id`, `monster_id`, `quantity`, `sort`)
SELECT 'wm_1_4_1_1', 'wave_1_4_1', 'mon_orc', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_wave_monster` WHERE `id` = 'wm_1_4_1_1');
