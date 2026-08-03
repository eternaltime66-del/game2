-- 清理引用已删除成品技能的孤儿绑定（扳机槽 / 完整技能组 / 效果残留）
SET NAMES utf8mb4;

DELETE ts FROM app_game_trigger_slot ts
LEFT JOIN app_game_finished_skill fs ON fs.id = ts.finished_skill_id
WHERE fs.id IS NULL;

DELETE ts FROM app_game_trigger_slot ts
LEFT JOIN app_game_finished_skill fs ON fs.id = ts.trigger_ref_id
WHERE ts.trigger_ref_id IS NOT NULL
  AND ts.trigger_ref_id != ''
  AND fs.id IS NULL;

DELETE cs FROM app_game_complete_skill cs
LEFT JOIN app_game_finished_skill fs ON fs.id = cs.finished_skill_id
WHERE fs.id IS NULL;

DELETE cs FROM app_game_complete_skill cs
LEFT JOIN app_game_finished_skill fs ON fs.id = cs.trigger_ref_id
WHERE cs.trigger_ref_id IS NOT NULL
  AND cs.trigger_ref_id != ''
  AND fs.id IS NULL;

DELETE e FROM app_game_finished_skill_effect e
LEFT JOIN app_game_finished_skill fs ON fs.id = e.finished_skill_id
WHERE fs.id IS NULL;
