-- 为现有波次怪物写入合理站位（左上角）；战斗仍会自动避让冲突
-- 行 0 = 前排，列 0-3；quantity>1 时仅第一个实例用此站位，其余自动找空位
SET NAMES utf8mb4;

-- 1-1 波：2 史莱姆前排居中偏左
UPDATE app_game_wave_monster SET slot_col = 1, slot_row = 0 WHERE id = 'wm_1_1_1';

-- 1-2 波：哥布林 + 史莱姆 前排
UPDATE app_game_wave_monster SET slot_col = 1, slot_row = 0 WHERE id = 'wm_1_2_1_1';
UPDATE app_game_wave_monster SET slot_col = 2, slot_row = 0 WHERE id = 'wm_1_2_1_2';

-- 1-3 波：2 哥布林前排
UPDATE app_game_wave_monster SET slot_col = 1, slot_row = 0 WHERE id = 'wm_1_3_1_1';

-- 1-4 波：兽人精英 前排居中 2×1
UPDATE app_game_wave_monster SET slot_col = 1, slot_row = 0 WHERE id = 'wm_1_4_1_1';

-- 用户自建：纯小怪群 → 首只前排左，其余自动铺开
UPDATE app_game_wave_monster SET slot_col = 0, slot_row = 0
WHERE monster_id IN ('mon_slime', 'mon_goblin') AND quantity >= 3 AND slot_col IS NULL;

-- Boss 后排居中
UPDATE app_game_wave_monster wm
INNER JOIN app_game_monster m ON m.id = wm.monster_id AND m.rank_type = 'BOSS'
SET wm.slot_col = 1, wm.slot_row = 1
WHERE wm.slot_col IS NULL;

-- 精英前排居中
UPDATE app_game_wave_monster wm
INNER JOIN app_game_monster m ON m.id = wm.monster_id AND m.rank_type = 'ELITE'
SET wm.slot_col = 1, wm.slot_row = 0
WHERE wm.slot_col IS NULL;

-- 与精英/Boss 同波的小怪 → 后排（用派生表避免 MySQL 1093）
UPDATE app_game_wave_monster wm
INNER JOIN app_game_monster m ON m.id = wm.monster_id AND m.rank_type = 'NORMAL'
INNER JOIN (
  SELECT DISTINCT wm2.wave_id
  FROM app_game_wave_monster wm2
  INNER JOIN app_game_monster m2 ON m2.id = wm2.monster_id
  WHERE m2.rank_type IN ('ELITE', 'BOSS')
) elite_waves ON elite_waves.wave_id = wm.wave_id
SET wm.slot_col = 0, wm.slot_row = 1
WHERE wm.slot_col IS NULL;

-- 其余仍为空：前排左
UPDATE app_game_wave_monster SET slot_col = 0, slot_row = 0 WHERE slot_col IS NULL;
