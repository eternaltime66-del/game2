SET NAMES utf8mb4;

UPDATE `app_game_item` SET `name` = CAST(UNHEX('E696B0E9B29CE6A091E58FB6') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/leaf.png' WHERE `id` = 'item_leaf';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E7B297E69CA8E6A38D') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/stick.png' WHERE `id` = 'item_stick';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E58FB2E88EB1E5A786E5879DE883B6') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/slime_gel.png' WHERE `id` = 'item_slime_gel';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E593A5E5B883E69E97E78DA0E78999') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/goblin_tooth.png' WHERE `id` = 'item_goblin_tooth';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E7A0B4E697A7E5B883E69699') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/rag.png' WHERE `id` = 'item_rag';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E9939CE5B881') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/copper_coin.png' WHERE `id` = 'item_copper_coin';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E585BDE4BABAE9AAA8E78987') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/orc_bone.png' WHERE `id` = 'item_orc_bone';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E7B297E588B6E79AAEE99DA9') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/rough_leather.png' WHERE `id` = 'item_rough_leather';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E99381E79FBFE79FB3') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/iron_ore.png' WHERE `id` = 'item_iron_ore';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E5A4B4E79BAEE5BEBDE7ABA0') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/boss_badge.png' WHERE `id` = 'item_boss_badge';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E7A880E69C89E6B0B4E699B6') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/rare_crystal.png' WHERE `id` = 'item_rare_crystal';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E98791E5B881') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/gold_coin.png' WHERE `id` = 'item_gold_coin';
UPDATE `app_game_item` SET `name` = CAST(UNHEX('E69CA8E794B2') AS CHAR CHARACTER SET utf8mb4), `icon` = '/img/items/wood_armor.png', `item_tags` = 'ARMOR' WHERE `id` = 'item_wood_armor';

UPDATE `app_game_item_log` l INNER JOIN `app_game_item` i ON l.`item_id` = i.`id` SET l.`item_name` = i.`name`;
