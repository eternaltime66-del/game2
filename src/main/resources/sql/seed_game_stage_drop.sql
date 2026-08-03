SET NAMES utf8mb4;

-- 材料来源：前往出击（关卡掉落配置）
INSERT INTO `app_game_stage_drop` (`id`, `stage_id`, `item_id`, `enabled`, `remark`)
SELECT 'sd_st_1_1_gel', 'st_1_1', 'item_slime_gel', 1, '1-1 史莱姆凝胶'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage_drop` WHERE `id` = 'sd_st_1_1_gel');

INSERT INTO `app_game_stage_drop` (`id`, `stage_id`, `item_id`, `enabled`, `remark`)
SELECT 'sd_st_1_1_leaf', 'st_1_1', 'item_leaf', 1, '1-1 新鲜树叶'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage_drop` WHERE `id` = 'sd_st_1_1_leaf');

INSERT INTO `app_game_stage_drop` (`id`, `stage_id`, `item_id`, `enabled`, `remark`)
SELECT 'sd_st_1_2_stick', 'st_1_2', 'item_stick', 1, '1-2 粗木棍'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage_drop` WHERE `id` = 'sd_st_1_2_stick');

INSERT INTO `app_game_stage_drop` (`id`, `stage_id`, `item_id`, `enabled`, `remark`)
SELECT 'sd_st_1_3_goblin_tooth', 'st_1_3', 'item_goblin_tooth', 1, '1-3 哥布林牙'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage_drop` WHERE `id` = 'sd_st_1_3_goblin_tooth');

INSERT INTO `app_game_stage_drop` (`id`, `stage_id`, `item_id`, `enabled`, `remark`)
SELECT 'sd_st_2_1_orc_bone', 'st_2_1', 'item_orc_bone', 1, '2-1 兽骨'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_stage_drop` WHERE `id` = 'sd_st_2_1_orc_bone');
