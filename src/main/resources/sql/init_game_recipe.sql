SET NAMES utf8mb4;

-- ========== 合成配方：产出物品 + 材料清单 ==========
CREATE TABLE IF NOT EXISTS `app_game_recipe` (
  `id` varchar(32) NOT NULL COMMENT '配方ID',
  `output_item_id` varchar(32) NOT NULL COMMENT '产出物品ID',
  `sort` int NOT NULL DEFAULT 0,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_output_item` (`output_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合成配方';

CREATE TABLE IF NOT EXISTS `app_game_recipe_material` (
  `id` varchar(32) NOT NULL,
  `recipe_id` varchar(32) NOT NULL,
  `material_item_id` varchar(32) NOT NULL COMMENT '材料物品ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '所需数量',
  `sort` int NOT NULL DEFAULT 0,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`),
  UNIQUE KEY `uk_recipe_material` (`recipe_id`, `material_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配方材料';

-- 木甲：木棍×2 + 史莱姆凝胶×4
INSERT INTO `app_game_recipe` (`id`, `output_item_id`, `sort`, `enabled`, `remark`)
SELECT 'rcp_wood_armor', 'item_wood_armor', 1, 1, '木棍×2 + 凝胶×4'
WHERE NOT EXISTS (SELECT 1 FROM `app_game_recipe` WHERE `id` = 'rcp_wood_armor');

INSERT INTO `app_game_recipe_material` (`id`, `recipe_id`, `material_item_id`, `quantity`, `sort`)
SELECT 'rcp_wood_armor_mat_stick', 'rcp_wood_armor', 'item_stick', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `app_game_recipe_material` WHERE `id` = 'rcp_wood_armor_mat_stick');

INSERT INTO `app_game_recipe_material` (`id`, `recipe_id`, `material_item_id`, `quantity`, `sort`)
SELECT 'rcp_wood_armor_mat_gel', 'rcp_wood_armor', 'item_slime_gel', 4, 2
WHERE NOT EXISTS (SELECT 1 FROM `app_game_recipe_material` WHERE `id` = 'rcp_wood_armor_mat_gel');
