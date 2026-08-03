SET NAMES utf8mb4;

-- 已有库补唯一约束：同一配方下材料不可重复
ALTER TABLE `app_game_recipe_material`
  ADD UNIQUE KEY `uk_recipe_material` (`recipe_id`, `material_item_id`);
