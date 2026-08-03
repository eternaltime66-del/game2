-- 装备扳机槽：单场释放次数上限（NULL=无限）
ALTER TABLE `app_game_trigger_slot`
  ADD COLUMN `max_cast_count` int DEFAULT NULL COMMENT '单场最多释放次数，NULL=无限' AFTER `finished_skill_id`;
