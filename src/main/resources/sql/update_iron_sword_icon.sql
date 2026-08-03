SET NAMES utf8mb4;

-- 铁剑图标：/img/items/iron_sword.png
UPDATE `app_game_item`
SET `icon` = '/img/items/iron_sword.png',
    `code` = IF(`code` IS NULL OR `code` = '', 'IRON_SWORD', `code`)
WHERE `id` = 'item_WXVt3xd8';
