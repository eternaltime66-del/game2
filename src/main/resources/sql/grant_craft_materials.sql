-- 涓烘祴璇曡处鍙峰彂鏀炬湪鐢插悎鎴愭潗鏂?-- 閰嶆柟锛氭湪妫嵜? + 鍑濊兌脳4
-- 榛樿 uid锛?728492138

SET NAMES utf8mb4;
SET @uid = '8728492138';

-- 鍙茶幈濮嗗嚌鑳?脳6锛堣冻澶熷悎鎴?1 娆★紝杩樺 2 涓級
INSERT INTO app_game_inventory (id, uid, slot_no, item_id, quantity)
SELECT 'inv_mat_gel_8728492138', @uid, 2, 'item_slime_gel', 6
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM app_game_inventory WHERE uid = @uid AND item_id = 'item_slime_gel'
);

UPDATE app_game_inventory
SET quantity = GREATEST(IFNULL(quantity, 0), 6)
WHERE uid = @uid AND item_id = 'item_slime_gel';

-- 绮楁湪妫?脳5
INSERT INTO app_game_inventory (id, uid, slot_no, item_id, quantity)
SELECT 'inv_mat_stick_8728492138', @uid, 3, 'item_stick', 5
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM app_game_inventory WHERE uid = @uid AND item_id = 'item_stick'
);

UPDATE app_game_inventory
SET quantity = GREATEST(IFNULL(quantity, 0), 5)
WHERE uid = @uid AND item_id = 'item_stick';
