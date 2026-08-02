SET NAMES utf8mb4;

UPDATE `app_game_item` SET `item_tags` = 'MATERIAL,WEAPON' WHERE `code` = 'STICK';
UPDATE `app_game_item` SET `item_tags` = 'MATERIAL' WHERE `code` <> 'STICK';
