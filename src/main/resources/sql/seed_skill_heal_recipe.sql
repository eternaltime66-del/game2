SET NAMES utf8mb4;

-- 小治疗术：补齐技能图标 + 合成配方材料
UPDATE `app_game_item`
SET `icon` = '/img/items/skill_heal.png'
WHERE `id` = 'item_sk_gqxocpja';

INSERT INTO `app_game_recipe` (`id`, `output_item_id`, `sort`, `enabled`, `remark`)
SELECT 'rcp_skill_heal', 'item_sk_gqxocpja', 2, 1, '树叶×3 + 凝胶×5 + 布料×1'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_recipe` WHERE `id` = 'rcp_skill_heal');

INSERT INTO `app_game_recipe_material` (`id`, `recipe_id`, `material_item_id`, `quantity`, `sort`)
SELECT 'rcp_skill_heal_leaf', 'rcp_skill_heal', 'item_leaf', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_recipe_material` WHERE `id` = 'rcp_skill_heal_leaf');

INSERT INTO `app_game_recipe_material` (`id`, `recipe_id`, `material_item_id`, `quantity`, `sort`)
SELECT 'rcp_skill_heal_gel', 'rcp_skill_heal', 'item_slime_gel', 5, 2
WHERE NOT EXISTS (SELECT 1 FROM `app_game_recipe_material` WHERE `id` = 'rcp_skill_heal_gel');

INSERT INTO `app_game_recipe_material` (`id`, `recipe_id`, `material_item_id`, `quantity`, `sort`)
SELECT 'rcp_skill_heal_rag', 'rcp_skill_heal', 'item_rag', 1, 3
WHERE NOT EXISTS (SELECT 1 FROM `app_game_recipe_material` WHERE `id` = 'rcp_skill_heal_rag');
