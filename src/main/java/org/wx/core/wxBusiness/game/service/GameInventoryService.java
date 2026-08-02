package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.mapper.GameInventoryMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameInventoryService extends WxServiceImpl<GameInventoryMapper, GameInventory> {

    @Resource
    private GameWarehouseService warehouseService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameItemLogService itemLogService;

    public WarehouseVo getWarehouseDetail(String uid) {
        GameWarehouse warehouse = warehouseService.getOrInit(uid);
        int maxSlots = warehouse.getMaxSlots() != null ? warehouse.getMaxSlots() : GameWarehouse.DEFAULT_MAX_SLOTS;
        List<GameInventory> rows = listByUid(uid);
        Map<Integer, GameInventory> slotMap = rows.stream()
                .collect(Collectors.toMap(GameInventory::getSlotNo, r -> r, (a, b) -> a));

        Map<String, GameItem> itemMap = loadItemMap(rows);
        WarehouseVo vo = new WarehouseVo();
        vo.setMaxSlots(maxSlots);
        vo.setUsedSlots(rows.size());

        List<InventorySlotVo> slots = new ArrayList<>();
        for (int slotNo = 1; slotNo <= maxSlots; slotNo++) {
            GameInventory row = slotMap.get(slotNo);
            InventorySlotVo slot = new InventorySlotVo();
            slot.setSlotNo(slotNo);
            if (row == null) {
                slot.setEmpty(true);
                slots.add(slot);
                continue;
            }
            GameItem item = itemMap.get(row.getItemId());
            slot.setEmpty(false);
            slot.setItemId(row.getItemId());
            slot.setQuantity(row.getQuantity());
            if (item != null) {
                slot.setItemCode(item.getCode());
                slot.setItemName(item.getName());
                slot.setIcon(item.getIcon());
                slot.setMaxStack(item.getMaxStack());
                slot.setUnitWeight(item.getWeight());
                ItemTagHelper.fillTags(slot, item);
            }
            slots.add(slot);
        }
        vo.setSlots(slots);
        vo.setTagFilters(buildWarehouseTagFilters(slots));
        return vo;
    }

    private List<ItemTagFilterVo> buildWarehouseTagFilters(List<InventorySlotVo> slots) {
        Map<GameItemTag, Integer> counts = new EnumMap<>(GameItemTag.class);
        for (InventorySlotVo slot : slots) {
            if (Boolean.TRUE.equals(slot.getEmpty())) {
                continue;
            }
            List<String> codes = slot.getItemTagCodes();
            if (codes == null || codes.isEmpty()) {
                continue;
            }
            Set<GameItemTag> unique = new LinkedHashSet<>();
            for (String code : codes) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                try {
                    unique.add(GameItemTag.valueOf(code.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // skip unknown tag
                }
            }
            for (GameItemTag tag : unique) {
                counts.merge(tag, 1, Integer::sum);
            }
        }
        return GameItemTag.allSorted().stream()
                .map(tag -> {
                    ItemTagFilterVo filter = new ItemTagFilterVo();
                    filter.setCode(tag.name());
                    filter.setLabel(tag.getLabel());
                    filter.setCount(counts.getOrDefault(tag, 0));
                    return filter;
                })
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public void grantBattleLoot(String uid, List<BattleLootEntry> lootList, String battleId) {
        if (lootList == null || lootList.isEmpty()) {
            return;
        }
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (BattleLootEntry entry : lootList) {
            if (entry.getItemId() == null || entry.getQuantity() == null || entry.getQuantity() <= 0) {
                continue;
            }
            merged.merge(entry.getItemId(), entry.getQuantity(), Integer::sum);
        }
        for (Map.Entry<String, Integer> e : merged.entrySet()) {
            addWarehouseItem(uid, e.getKey(), e.getValue(), GameItemLog.REASON_BATTLE_WIN, battleId, "战斗胜利发放");
        }
    }

    public GameInventory findWarehouseSlot(String uid, Integer slotNo) {
        return this.find()
                .eq(GameInventory::getUid, uid)
                .eq(GameInventory::getSlotNo, slotNo)
                .one();
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeWarehouseQuantity(String uid, Integer slotNo, int quantity, String reason, String remark) {
        GameInventory row = findWarehouseSlot(uid, slotNo);
        ErrorFactory.notNull(row, "仓库格子为空");
        GameItem item = gameItemService.getById(row.getItemId());
        String itemName = item != null ? item.getName() : row.getItemId();
        int before = row.getQuantity() != null ? row.getQuantity() : 0;
        int after = before - quantity;
        ErrorFactory.throwError(after < 0, "仓库数量不足");
        if (after == 0) {
            this.removeById(row.getId());
        } else {
            row.setQuantity(after);
            this.updateById(row);
        }
        saveItemLog(uid, row.getItemId(), itemName, -quantity, before, after, reason, String.valueOf(slotNo), remark);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addWarehouseItem(String uid, String itemId, int quantity, String reason, String refId, String remark) {
        addItem(uid, itemId, quantity, reason, refId, remark);
    }

    public void saveItemLog(String uid, String itemId, String itemName, int changeQty,
                            int beforeQty, int afterQty, String reason, String refId, String remark) {
        itemLogService.save(GameItemLog.of(uid, itemId, itemName, changeQty, beforeQty, afterQty, reason, refId, remark));
    }

    public Map<String, Integer> countWarehouseItems(String uid) {
        List<GameInventory> rows = listByUid(uid);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (GameInventory row : rows) {
            if (row.getItemId() == null) {
                continue;
            }
            int qty = row.getQuantity() != null ? row.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            counts.merge(row.getItemId(), qty, Integer::sum);
        }
        return counts;
    }

    public int countWarehouseItem(String uid, String itemId) {
        return countWarehouseItems(uid).getOrDefault(itemId, 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeWarehouseItem(String uid, String itemId, int quantity, String reason, String refId, String remark) {
        ErrorFactory.throwError(quantity <= 0, "消耗数量无效");
        int owned = countWarehouseItem(uid, itemId);
        ErrorFactory.throwError(owned < quantity, "仓库材料不足");

        List<GameInventory> rows = listByUid(uid);
        int remaining = quantity;
        for (GameInventory row : rows) {
            if (remaining <= 0) {
                break;
            }
            if (!itemId.equals(row.getItemId())) {
                continue;
            }
            int current = row.getQuantity() != null ? row.getQuantity() : 0;
            if (current <= 0) {
                continue;
            }
            int take = Math.min(current, remaining);
            int after = current - take;
            GameItem item = gameItemService.getById(itemId);
            String itemName = item != null ? item.getName() : itemId;
            if (after == 0) {
                this.removeById(row.getId());
            } else {
                row.setQuantity(after);
                this.updateById(row);
            }
            saveItemLog(uid, itemId, itemName, -take, current, after, reason, refId, remark);
            remaining -= take;
        }
        ErrorFactory.throwError(remaining > 0, "仓库材料不足");
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public void discardSlots(String uid, List<Integer> slotNos) {
        ErrorFactory.notNull(slotNos, "请选择要丢弃的格子");
        ErrorFactory.throwError(slotNos.isEmpty(), "请选择要丢弃的格子");

        List<GameInventory> rows = listByUid(uid);
        Map<Integer, GameInventory> slotMap = rows.stream()
                .collect(Collectors.toMap(GameInventory::getSlotNo, r -> r, (a, b) -> a));
        Map<String, GameItem> itemMap = loadItemMap(rows);

        Set<Integer> unique = new LinkedHashSet<>(slotNos);
        for (Integer slotNo : unique) {
            GameInventory row = slotMap.get(slotNo);
            if (row == null) {
                continue;
            }
            GameItem item = itemMap.get(row.getItemId());
            String itemName = item != null ? item.getName() : row.getItemId();
            int qty = row.getQuantity() != null ? row.getQuantity() : 0;
            itemLogService.save(GameItemLog.of(
                    uid,
                    row.getItemId(),
                    itemName,
                    -qty,
                    qty,
                    0,
                    GameItemLog.REASON_DISCARD,
                    String.valueOf(slotNo),
                    "仓库丢弃"
            ));
            this.removeById(row.getId());
        }
    }

    private void addItem(String uid, String itemId, int quantity, String reason, String refId, String remark) {
        if (quantity <= 0) {
            return;
        }
        GameItem item = gameItemService.getById(itemId);
        ErrorFactory.notNull(item, "物品不存在");
        ErrorFactory.throwError(!Integer.valueOf(1).equals(item.getEnabled()), "物品未启用");

        GameWarehouse warehouse = warehouseService.getOrInit(uid);
        int maxSlots = warehouse.getMaxSlots() != null ? warehouse.getMaxSlots() : GameWarehouse.DEFAULT_MAX_SLOTS;
        int maxStack = item.getMaxStack() != null ? item.getMaxStack() : 99;

        List<GameInventory> rows = listByUid(uid);
        int remaining = quantity;

        for (GameInventory row : rows) {
            if (remaining <= 0) {
                break;
            }
            if (!itemId.equals(row.getItemId())) {
                continue;
            }
            int current = row.getQuantity() != null ? row.getQuantity() : 0;
            if (current >= maxStack) {
                continue;
            }
            int canAdd = Math.min(maxStack - current, remaining);
            int before = current;
            int after = current + canAdd;
            row.setQuantity(after);
            this.updateById(row);
            itemLogService.save(GameItemLog.of(uid, itemId, item.getName(), canAdd, before, after, reason, refId, remark));
            remaining -= canAdd;
        }

        Set<Integer> usedSlots = rows.stream().map(GameInventory::getSlotNo).collect(Collectors.toSet());
        while (remaining > 0) {
            int slotNo = findNextEmptySlot(usedSlots, maxSlots);
            ErrorFactory.notNull(slotNo, "仓库已满，无法放入更多物品");

            int put = Math.min(maxStack, remaining);
            GameInventory row = new GameInventory();
            row.setUid(uid);
            row.setSlotNo(slotNo);
            row.setItemId(itemId);
            row.setQuantity(put);
            this.save(row);
            usedSlots.add(slotNo);
            itemLogService.save(GameItemLog.of(uid, itemId, item.getName(), put, 0, put, reason, refId, remark));
            remaining -= put;
        }
    }

    private List<GameInventory> listByUid(String uid) {
        return this.find()
                .eq(GameInventory::getUid, uid)
                .orderByAsc(GameInventory::getSlotNo)
                .list();
    }

    private Map<String, GameItem> loadItemMap(List<GameInventory> rows) {
        Set<String> itemIds = rows.stream().map(GameInventory::getItemId).collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return gameItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(GameItem::getId, i -> i, (a, b) -> a));
    }

    private Integer findNextEmptySlot(Set<Integer> usedSlots, int maxSlots) {
        for (int slotNo = 1; slotNo <= maxSlots; slotNo++) {
            if (!usedSlots.contains(slotNo)) {
                return slotNo;
            }
        }
        return null;
    }
}
