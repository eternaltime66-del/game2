-- 角色模板可绑定多个职业
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_game_character_profession` (
  `id` varchar(32) NOT NULL,
  `character_template_id` varchar(32) NOT NULL,
  `profession_id` varchar(32) NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_char_prof` (`character_template_id`,`profession_id`),
  KEY `idx_character` (`character_template_id`),
  KEY `idx_profession` (`profession_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色模板-职业绑定(多对多)';
