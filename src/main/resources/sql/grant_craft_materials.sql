-- 为测试账号发放木甲合成材料
-- 配方：木棍×2 + 凝胶×4
-- 默认 uid：8728492138

SET NAMES utf8mb4;
SET @uid = '8728492138';

-- 史莱姆凝胶 ×6（足够合成 1 次，还多 2 个）
INSERT INTO app_game_inventory (id, uid, slot_no, item_id, quantity)
SELECT 'inv_mat_gel_8728492138', @uid, 2, 'item_slime_gel', 6
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM app_game_inventory WHERE uid = @uid AND item_id = 'item_slime_gel'
);

UPDATE app_game_inventory
SET quantity = GREATEST(IFNULL(quantity, 0), 6)
WHERE uid = @uid AND item_id = 'item_slime_gel';

-- 粗木棍 ×5
INSERT INTO app_game_inventory (id, uid, slot_no, item_id, quantity)
SELECT 'inv_mat_stick_8728492138', @uid, 3, 'item_stick', 5
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM app_game_inventory WHERE uid = @uid AND item_id = 'item_stick'
);

UPDATE app_game_inventory
SET quantity = GREATEST(IFNULL(quantity, 0), 5)
WHERE uid = @uid AND item_id = 'item_stick';
