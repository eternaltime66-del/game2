package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.HeroEquipSlot;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamePrepService {

    @Resource
    private GameHeroService gameHeroService;
    @Resource
    private GameBattleBagService battleBagService;
    @Resource
    private GameInventoryService inventoryService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private CarryWeightService carryWeightService;
    @Resource
    private HeroCombatService heroCombatService;
    @Resource
    private GameHeroEquipService heroEquipService;
    @Resource
    private HeroPassiveService heroPassiveService;
    @Resource
    private BasicAttackService basicAttackService;
    @Resource
    private WeaponSkillService weaponSkillService;

    public PrepSummaryVo getPrepSummary(String uid) {
        GameHero hero = gameHeroService.getOrInitHero(uid);
        BattleBagVo battleBag = getBattleBag(uid, hero);
        syncOutsideBattleHero(uid, hero, battleBag.getTotalMaxHp());
        PrepSummaryVo vo = new PrepSummaryVo();
        vo.setHero(hero);
        vo.setBattleBag(battleBag);
        vo.setWarehouse(inventoryService.getWarehouseDetail(uid));
        return vo;
    }

    public BattleBagVo getBattleBag(String uid) {
        GameHero hero = gameHeroService.getOrInitHero(uid);
        return getBattleBag(uid, hero);
    }

    public GameHero getOutsideBattleHero(String uid) {
        GameHero hero = gameHeroService.getOrInitHero(uid);
        int totalMaxHp = resolveBattleMaxHp(uid);
        gameHeroService.syncOutsideBattleHp(hero, totalMaxHp);
        return hero;
    }

    public BattleBagVo getBattleBag(String uid, GameHero hero) {
        List<GameBattleBag> rows = battleBagService.listByUid(uid);
        List<String> itemIds = new ArrayList<>(rows.stream().map(GameBattleBag::getItemId).distinct().toList());
        GameHeroEquip equip = heroEquipService.getOrInit(uid);
        for (String equippedId : equip.listEquippedItemIds()) {
            if (!itemIds.contains(equippedId)) {
                itemIds.add(equippedId);
            }
        }
        Map<String, GameItem> itemMap = loadItemMap(itemIds);
        HeroCombatService.HeroCombatContext combat = heroCombatService.resolve(uid, hero, rows, itemMap, equip);

        BattleBagVo weightVo = carryWeightService.buildWeightSummary(hero, rows, new ArrayList<>(itemMap.values()), combat);
        List<String> equippedIds = combat != null
                ? new ArrayList<>(combat.getEquippedItemIds())
                : equip.listEquippedItemIds();
        weightVo.setBasicAttackSkill(basicAttackService.resolveDetail(equippedIds));
        weightVo.setBasicAttackSourceLabel(basicAttackService.resolveSourceLabel(equippedIds));
        weightVo.setWeaponSkill(weaponSkillService.resolveTrigger(equippedIds));
        weightVo.setWeaponSkillSourceLabel(weaponSkillService.resolveSourceLabel(equippedIds));
        weightVo.setEquipSlots(heroEquipService.buildSlotOverview(equip, itemMap, combat));
        weightVo.setActivePassives(heroPassiveService.buildHeroPassiveDetails(
                equip, combat.getEquippedItemIds(), itemMap));
        weightVo.setStatBreakdown(heroCombatService.buildStatBreakdown(combat, weightVo.getEffectiveActionValue()));
        List<BattleBagItemVo> items = new ArrayList<>();
        for (GameBattleBag row : rows) {
            int qty = row.getQuantity() != null ? row.getQuantity() : 0;
            if (qty <= 0) continue;
            GameItem item = itemMap.get(row.getItemId());
            BattleBagItemVo itemVo = new BattleBagItemVo();
            itemVo.setId(row.getId());
            itemVo.setItemId(row.getItemId());
            itemVo.setQuantity(qty);
            itemVo.setSort(row.getSort());
            if (item != null) {
                itemVo.setItemCode(item.getCode());
                itemVo.setItemName(item.getName());
                itemVo.setIcon(item.getIcon());
                itemVo.setMaxStack(item.getMaxStack());
            }
            carryWeightService.fillItemWeight(itemVo, item, qty);
            ItemTagHelper.fillTags(itemVo, item);
            items.add(itemVo);
        }
        weightVo.setItems(items);
        return weightVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public void batchWarehouseToBag(String uid, List<Integer> slotNos) {
        ErrorFactory.throwError(slotNos == null || slotNos.isEmpty(), "请选择仓库格子");
        Set<Integer> unique = new LinkedHashSet<>(slotNos);
        for (Integer slotNo : unique) {
            moveWarehouseSlotToBag(uid, slotNo, null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public void batchBagToWarehouse(String uid, List<String> bagIds) {
        ErrorFactory.throwError(bagIds == null || bagIds.isEmpty(), "请选择背包物品");
        Set<String> unique = new LinkedHashSet<>(bagIds);
        for (String bagId : unique) {
            moveBagToWarehouse(uid, bagId, null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public void dragTransfer(String uid, String fromType, String fromKey, String toType, String toKey, Integer quantity) {
        ErrorFactory.notNull(fromType, "来源类型不能为空");
        ErrorFactory.notNull(toType, "目标类型不能为空");
        if ("WAREHOUSE".equals(fromType) && "BAG".equals(toType)) {
            moveWarehouseSlotToBag(uid, Integer.valueOf(fromKey), quantity);
            return;
        }
        if ("BAG".equals(fromType) && "WAREHOUSE".equals(toType)) {
            moveBagToWarehouse(uid, fromKey, quantity);
            return;
        }
        if ("BAG".equals(fromType) && "BAG".equals(toType)) {
            mergeBagItems(uid, fromKey, toKey, quantity);
            return;
        }
        ErrorFactory.throwError(true, "暂不支持该转移方式");
    }

    private void moveWarehouseSlotToBag(String uid, Integer slotNo, Integer quantity) {
        GameInventory row = inventoryService.findWarehouseSlot(uid, slotNo);
        ErrorFactory.notNull(row, "仓库格子为空");
        int available = row.getQuantity() != null ? row.getQuantity() : 0;
        ErrorFactory.throwError(available <= 0, "仓库格子为空");
        int moveQty = quantity == null || quantity <= 0 ? available : Math.min(quantity, available);

        inventoryService.removeWarehouseQuantity(uid, slotNo, moveQty, GameItemLog.REASON_TO_BAG, "移入战斗背包");
        addBagQuantity(uid, row.getItemId(), moveQty, GameItemLog.REASON_TO_BAG, String.valueOf(slotNo), "从仓库移入背包");
    }

    private void moveBagToWarehouse(String uid, String bagId, Integer quantity) {
        GameBattleBag row = battleBagService.getById(bagId);
        ErrorFactory.notNull(row, "背包物品不存在");
        ErrorFactory.notEquals(uid, row.getUid(), "背包物品不匹配");
        int available = row.getQuantity() != null ? row.getQuantity() : 0;
        ErrorFactory.throwError(available <= 0, "背包物品数量不足");
        int moveQty = quantity == null || quantity <= 0 ? available : Math.min(quantity, available);

        removeBagQuantity(uid, row, moveQty, GameItemLog.REASON_TO_WAREHOUSE, "移入仓库");
        inventoryService.addWarehouseItem(uid, row.getItemId(), moveQty, GameItemLog.REASON_TO_WAREHOUSE, bagId, "从背包移入仓库");
    }

    private void mergeBagItems(String uid, String fromBagId, String toBagId, Integer quantity) {
        if (fromBagId.equals(toBagId)) {
            return;
        }
        GameBattleBag from = battleBagService.getById(fromBagId);
        GameBattleBag to = battleBagService.getById(toBagId);
        ErrorFactory.notNull(from, "来源背包物品不存在");
        ErrorFactory.notNull(to, "目标背包物品不存在");
        ErrorFactory.notEquals(uid, from.getUid(), "来源不匹配");
        ErrorFactory.notEquals(uid, to.getUid(), "目标不匹配");
        ErrorFactory.notEquals(from.getItemId(), to.getItemId(), "只能合并相同物品");
        int available = from.getQuantity() != null ? from.getQuantity() : 0;
        int moveQty = quantity == null || quantity <= 0 ? available : Math.min(quantity, available);
        ErrorFactory.throwError(moveQty <= 0, "数量无效");

        from.setQuantity(available - moveQty);
        if (from.getQuantity() <= 0) {
            battleBagService.removeById(from.getId());
        } else {
            battleBagService.updateById(from);
        }
        to.setQuantity((to.getQuantity() != null ? to.getQuantity() : 0) + moveQty);
        battleBagService.updateById(to);
    }

    private void addBagQuantity(String uid, String itemId, int quantity, String reason, String refId, String remark) {
        if (quantity <= 0) return;
        GameItem item = gameItemService.getById(itemId);
        ErrorFactory.notNull(item, "物品不存在");

        GameBattleBag row = battleBagService.findByUidAndItemId(uid, itemId);
        int before = 0;
        if (row == null) {
            row = new GameBattleBag();
            row.setUid(uid);
            row.setItemId(itemId);
            row.setQuantity(quantity);
            row.setSort(nextBagSort(uid));
            battleBagService.save(row);
        } else {
            before = row.getQuantity() != null ? row.getQuantity() : 0;
            row.setQuantity(before + quantity);
            battleBagService.updateById(row);
        }
        inventoryService.saveItemLog(uid, itemId, item.getName(), quantity, before, before + quantity, reason, refId, remark);
    }

    private void removeBagQuantity(String uid, GameBattleBag row, int quantity, String reason, String remark) {
        GameItem item = gameItemService.getById(row.getItemId());
        String itemName = item != null ? item.getName() : row.getItemId();
        int before = row.getQuantity() != null ? row.getQuantity() : 0;
        int after = before - quantity;
        ErrorFactory.throwError(after < 0, "背包数量不足");
        if (after == 0) {
            battleBagService.removeById(row.getId());
        } else {
            row.setQuantity(after);
            battleBagService.updateById(row);
        }
        inventoryService.saveItemLog(uid, row.getItemId(), itemName, -quantity, before, after, reason, row.getId(), remark);
    }

    private int nextBagSort(String uid) {
        List<GameBattleBag> rows = battleBagService.listByUid(uid);
        return rows.stream().map(GameBattleBag::getSort).filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
    }

    private Map<String, GameItem> loadItemMap(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return gameItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(GameItem::getId, i -> i, (a, b) -> a));
    }

    public int resolveBattleActionValue(String uid) {
        return resolveBattleStats(uid).getEffectiveActionValue();
    }

    public int resolveBattleAttack(String uid) {
        return resolveBattleStats(uid).getNormalAttackDamage();
    }

    public int resolveBattleTotalAttack(String uid) {
        return resolveBattleStats(uid).getTotalAttack();
    }

    public int resolveBattleDefense(String uid) {
        return resolveBattleStats(uid).getTotalDefense();
    }

    public int resolveBattleMaxHp(String uid) {
        return resolveBattleStats(uid).getTotalMaxHp();
    }

    public BattleBagVo resolveBattleStats(String uid) {
        GameHero hero = gameHeroService.getOrInitHero(uid);
        List<GameBattleBag> rows = battleBagService.listByUid(uid);
        List<String> itemIds = new ArrayList<>(rows.stream().map(GameBattleBag::getItemId).distinct().toList());
        GameHeroEquip equip = heroEquipService.getOrInit(uid);
        for (String equippedId : equip.listEquippedItemIds()) {
            if (!itemIds.contains(equippedId)) {
                itemIds.add(equippedId);
            }
        }
        Map<String, GameItem> itemMap = loadItemMap(itemIds);
        HeroCombatService.HeroCombatContext combat = heroCombatService.resolve(uid, hero, rows, itemMap, equip);
        return carryWeightService.buildWeightSummary(hero, rows, new ArrayList<>(itemMap.values()), combat);
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public BattleBagVo equipWeapon(String uid, String itemId) {
        heroEquipService.equipWeapon(uid, itemId);
        return finishEquipChange(uid);
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public BattleBagVo unequipWeapon(String uid) {
        heroEquipService.unequipWeapon(uid);
        return finishEquipChange(uid);
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public BattleBagVo equipSlot(String uid, String slot, String itemId) {
        HeroEquipSlot equipSlot = HeroEquipSlot.parse(slot);
        ErrorFactory.notNull(equipSlot, "无效的装备槽位");
        heroEquipService.equipSlot(uid, equipSlot, itemId);
        return finishEquipChange(uid);
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public BattleBagVo unequipSlot(String uid, String slot) {
        HeroEquipSlot equipSlot = HeroEquipSlot.parse(slot);
        ErrorFactory.notNull(equipSlot, "无效的装备槽位");
        heroEquipService.unequipSlot(uid, equipSlot);
        return finishEquipChange(uid);
    }

    private BattleBagVo finishEquipChange(String uid) {
        gameHeroService.persistOutsideBattleHp(uid, resolveBattleMaxHp(uid));
        return getBattleBag(uid);
    }

    private void syncOutsideBattleHero(String uid, GameHero hero, Integer totalMaxHp) {
        if (totalMaxHp == null) {
            totalMaxHp = resolveBattleMaxHp(uid);
        }
        gameHeroService.syncOutsideBattleHp(hero, totalMaxHp);
        gameHeroService.persistOutsideBattleHp(uid, totalMaxHp);
    }

    public HeroFormationVo getHeroFormation(String uid) {
        GameHero hero = gameHeroService.getOrInitHero(uid);
        return toFormationVo(hero);
    }

    @Transactional(rollbackFor = Exception.class)
    public HeroFormationVo saveHeroFormation(String uid, Integer slotCol, Integer slotRow) {
        ErrorFactory.notNull(slotCol, "站位列不能为空");
        ErrorFactory.notNull(slotRow, "站位行不能为空");
        int w = BattleFormation.HERO_FOOTPRINT_W;
        int h = BattleFormation.HERO_FOOTPRINT_H;
        ErrorFactory.throwError(!BattleFormation.inBounds(slotCol, slotRow, w, h), "站位超出战场范围");

        GameHero hero = gameHeroService.getOrInitHero(uid);
        hero.setSlotCol(slotCol);
        hero.setSlotRow(slotRow);
        gameHeroService.updateById(hero);
        return toFormationVo(hero);
    }

    private HeroFormationVo toFormationVo(GameHero hero) {
        HeroFormationVo vo = new HeroFormationVo();
        vo.setName(hero.getName() != null ? hero.getName() : GameHero.DEFAULT_NAME);
        int w = BattleFormation.HERO_FOOTPRINT_W;
        int h = BattleFormation.HERO_FOOTPRINT_H;
        int col = hero.getSlotCol() != null ? hero.getSlotCol() : 1;
        int row = hero.getSlotRow() != null ? hero.getSlotRow() : 0;
        col = Math.max(0, Math.min(col, BattleFormation.COLS - w));
        row = Math.max(0, Math.min(row, BattleFormation.ROWS - h));
        vo.setSlotCol(col);
        vo.setSlotRow(row);
        vo.setFootprintW(w);
        vo.setFootprintH(h);
        vo.setCols(BattleFormation.COLS);
        vo.setRows(BattleFormation.ROWS);
        return vo;
    }
}
