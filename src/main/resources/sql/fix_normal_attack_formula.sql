SET NAMES utf8mb4;

UPDATE `app_game_finished_skill`
SET
  `target_type` = 'FIRST_TARGET',
  `target_param` = NULL,
  `hit_frequency` = 1,
  `max_cast_count` = NULL,
  `formulas_json` = '[{"outcome":"DAMAGE","targetType":"FIRST_TARGET","targetParam":null,"hitFrequency":1,"maxCastCount":null,"tokens":[{"kind":"READ","read":"CHAR_ATTACK","filter":null,"filterRef":null,"value":null,"op":null},{"kind":"OP","read":null,"filter":null,"filterRef":null,"value":null,"op":"*"},{"kind":"CONST","read":null,"filter":null,"filterRef":null,"value":1,"op":null},{"kind":"OP","read":null,"filter":null,"filterRef":null,"value":null,"op":"*"},{"kind":"READ","read":"WEAPON_DAMAGE_RATIO","filter":null,"filterRef":null,"value":null,"op":null}]}]',
  `remark` = '能量值满：首位目标，角色攻击力×1.0×武器伤害比例'
WHERE `id` = 'fin_normal_attack';

SELECT id, name, target_type, hit_frequency, formulas_json, remark
FROM app_game_finished_skill
WHERE id = 'fin_normal_attack';
