package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;
import org.wx.core.wxBusiness.game.entity.ItemTriggerSlotDetailVo;

import java.util.List;

@Service
public class WeaponSkillService {

    @Resource
    private GameWeaponService weaponService;
    @Resource
    private GameTriggerSlotService triggerSlotService;
    @Resource
    private GameItemService gameItemService;

    /** 备战面板武器主动槽：仅读取装备武器的大招槽 */
    public ItemTriggerSlotDetailVo resolveTrigger(List<String> equippedItemIds) {
        GameTriggerSlot slot = findWeaponUltimateSlot(equippedItemIds);
        if (slot == null) {
            return null;
        }
        return gameItemService.buildTriggerSlotDetailVo(slot);
    }

    public String resolveSourceLabel(List<String> equippedItemIds) {
        if (equippedItemIds == null) {
            return null;
        }
        for (String itemId : equippedItemIds) {
            if (weaponService.getByItemId(itemId) == null) {
                continue;
            }
            GameTriggerSlot slot = triggerSlotService.findUltimateSlot(itemId);
            if (slot == null || !Integer.valueOf(1).equals(slot.getEnabled())
                    || slot.getFinishedSkillId() == null || slot.getFinishedSkillId().isBlank()) {
                continue;
            }
            GameItem item = gameItemService.getById(itemId);
            return item != null ? "武器·" + item.getName() : "武器";
        }
        return null;
    }

    private GameTriggerSlot findWeaponUltimateSlot(List<String> equippedItemIds) {
        if (equippedItemIds == null) {
            return null;
        }
        for (String itemId : equippedItemIds) {
            if (weaponService.getByItemId(itemId) == null) {
                continue;
            }
            GameTriggerSlot slot = triggerSlotService.findUltimateSlot(itemId);
            if (slot != null && Integer.valueOf(1).equals(slot.getEnabled())
                    && slot.getFinishedSkillId() != null && !slot.getFinishedSkillId().isBlank()) {
                return slot;
            }
        }
        return null;
    }
}
