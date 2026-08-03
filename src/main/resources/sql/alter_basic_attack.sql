SET NAMES utf8mb4;

ALTER TABLE `app_game_weapon`
  ADD COLUMN `basic_attack_finished_skill_id` varchar(32) DEFAULT NULL
  COMMENT '普攻成品技能ID，空=角色默认' AFTER `damage_ratio`;

ALTER TABLE `app_game_finished_skill`
  ADD COLUMN `skill_category` varchar(32) NOT NULL DEFAULT 'ACTIVE'
  COMMENT 'ACTIVE=主动技能 BASIC_ATTACK=基础普攻' AFTER `target_param`;

UPDATE `app_game_finished_skill`
SET `target_type` = 'FRONT_ROW_RANDOM_ONE_ENEMY',
    `skill_category` = 'BASIC_ATTACK',
    `remark` = '行动值满：前排随机1敌方，攻击力100%×武器比例'
WHERE `id` = 'fin_normal_attack';
