SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `sp_drop_legacy_craft_recipe`;

DELIMITER //
CREATE PROCEDURE `sp_drop_legacy_craft_recipe`()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'app_game_craft_recipe'
  ) THEN
    INSERT INTO `app_game_recipe` (`id`, `output_item_id`, `sort`, `enabled`, `remark`)
    SELECT CONCAT('rcp_mig_', LOWER(cr.`code`)), cr.`result_item_id`, cr.`sort`, cr.`enabled`, cr.`remark`
    FROM `app_game_craft_recipe` cr
    WHERE NOT EXISTS (
      SELECT 1 FROM `app_game_recipe` r WHERE r.`output_item_id` = cr.`result_item_id`
    );

    INSERT INTO `app_game_recipe_material` (`id`, `recipe_id`, `material_item_id`, `quantity`, `sort`)
    SELECT
      CONCAT('rcp_mig_mat_', cm.`id`),
      r.`id`,
      cm.`item_id`,
      cm.`quantity`,
      cm.`sort`
    FROM `app_game_craft_material` cm
    INNER JOIN `app_game_craft_recipe` cr ON cr.`id` = cm.`recipe_id`
    INNER JOIN `app_game_recipe` r ON r.`output_item_id` = cr.`result_item_id`
    WHERE NOT EXISTS (
      SELECT 1 FROM `app_game_recipe_material` m
      WHERE m.`recipe_id` = r.`id` AND m.`material_item_id` = cm.`item_id`
    );

    DROP TABLE IF EXISTS `app_game_craft_material`;
    DROP TABLE IF EXISTS `app_game_craft_recipe`;
  END IF;
END//
DELIMITER ;

CALL `sp_drop_legacy_craft_recipe`();
DROP PROCEDURE IF EXISTS `sp_drop_legacy_craft_recipe`;
