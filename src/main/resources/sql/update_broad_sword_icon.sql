SET NAMES utf8mb4;

-- 阔剑图标：/img/items/broad_sword.png
UPDATE `app_game_item`
SET `icon` = '/img/items/broad_sword.png',
    `code` = IF(`code` IS NULL OR `code` = '', 'BROAD_SWORD', `code`)
WHERE `id` = 'item_f7frg3s0';
