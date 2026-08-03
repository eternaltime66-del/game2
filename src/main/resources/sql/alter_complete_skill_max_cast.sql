SET NAMES utf8mb4;

ALTER TABLE `app_game_complete_skill`
  ADD COLUMN `max_cast_count` int DEFAULT NULL COMMENT '单场最多释放次数，NULL=无限' AFTER `finished_skill_id`;
