SET NAMES utf8mb4;

-- 关卡掉落（材料来源：前往出击）
CREATE TABLE IF NOT EXISTS `app_game_stage_drop` (
  `id` varchar(32) NOT NULL,
  `stage_id` varchar(32) NOT NULL COMMENT '小关卡ID',
  `item_id` varchar(32) NOT NULL COMMENT '掉落物品ID',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stage_id` (`stage_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关卡掉落';
