package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;
import org.wx.core.wxBusiness.game.entity.enums.TriggerSlotKind;
import org.wx.core.wxBusiness.game.entity.enums.TriggerSlotType;
import org.wx.core.wxBusiness.game.mapper.GameTriggerSlotMapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameTriggerSlotService extends WxServiceImpl<GameTriggerSlotMapper, GameTriggerSlot> {

    public List<GameTriggerSlot> listByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Collections.emptyList();
        }
        return this.find()
                .eq(GameTriggerSlot::getItemId, itemId)
                .orderByAsc(GameTriggerSlot::getSort)
                .list();
    }

    public List<GameTriggerSlot> listByMonsterId(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return Collections.emptyList();
        }
        return this.find()
                .eq(GameTriggerSlot::getMonsterId, monsterId)
                .orderByAsc(GameTriggerSlot::getSort)
                .list();
    }

    public GameTriggerSlot findBasicAttackSlotByMonster(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return null;
        }
        GameTriggerSlot byKind = this.find()
                .eq(GameTriggerSlot::getMonsterId, monsterId)
                .eq(GameTriggerSlot::getSlotKind, TriggerSlotKind.BASIC_ATTACK.name())
                .one();
        if (byKind != null) {
            return byKind;
        }
        return this.find()
                .eq(GameTriggerSlot::getMonsterId, monsterId)
                .eq(GameTriggerSlot::getTriggerSlotType, TriggerSlotType.ACTION_VALUE_FULL.name())
                .one();
    }

    public GameTriggerSlot findUltimateSlotByMonster(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return null;
        }
        return this.find()
                .eq(GameTriggerSlot::getMonsterId, monsterId)
                .eq(GameTriggerSlot::getSlotKind, TriggerSlotKind.ULTIMATE.name())
                .one();
    }

    public String findBasicAttackSkillIdByMonster(String monsterId) {
        GameTriggerSlot slot = findBasicAttackSlotByMonster(monsterId);
        if (slot == null || !Integer.valueOf(1).equals(slot.getEnabled())) {
            return null;
        }
        String skillId = slot.getFinishedSkillId();
        if (skillId != null && !skillId.isBlank()) {
            return skillId;
        }
        return null;
    }

    public List<GameTriggerSlot> listCombatBindingsByMonsterId(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return Collections.emptyList();
        }
        return listEnabledByMonsterId(monsterId).stream()
                .filter(slot -> !TriggerSlotKind.isBasicAttack(slot))
                .collect(Collectors.toList());
    }

    public List<GameTriggerSlot> listEnabledByMonsterId(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return Collections.emptyList();
        }
        return this.find()
                .eq(GameTriggerSlot::getMonsterId, monsterId)
                .eq(GameTriggerSlot::getEnabled, 1)
                .orderByAsc(GameTriggerSlot::getSort)
                .list();
    }

    public GameTriggerSlot findBasicAttackSlot(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        GameTriggerSlot byKind = this.find()
                .eq(GameTriggerSlot::getItemId, itemId)
                .eq(GameTriggerSlot::getSlotKind, TriggerSlotKind.BASIC_ATTACK.name())
                .one();
        if (byKind != null) {
            return byKind;
        }
        return this.find()
                .eq(GameTriggerSlot::getItemId, itemId)
                .eq(GameTriggerSlot::getTriggerSlotType, TriggerSlotType.ACTION_VALUE_FULL.name())
                .one();
    }

    public GameTriggerSlot findUltimateSlot(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return this.find()
                .eq(GameTriggerSlot::getItemId, itemId)
                .eq(GameTriggerSlot::getSlotKind, TriggerSlotKind.ULTIMATE.name())
                .one();
    }

    public List<GameTriggerSlot> listTraitActiveByItemId(String itemId) {
        return listByItemId(itemId).stream()
                .filter(TriggerSlotKind::isTraitActive)
                .collect(Collectors.toList());
    }

    public String findBasicAttackSkillId(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return null;
        }
        for (String itemId : itemIds) {
            GameTriggerSlot slot = findBasicAttackSlot(itemId);
            if (slot == null || !Integer.valueOf(1).equals(slot.getEnabled())) {
                continue;
            }
            String skillId = slot.getFinishedSkillId();
            if (skillId != null && !skillId.isBlank()) {
                return skillId;
            }
        }
        return null;
    }

    public GameTriggerSlot findUltimateSlot(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return null;
        }
        for (String itemId : itemIds) {
            GameTriggerSlot slot = findUltimateSlot(itemId);
            if (slot != null && Integer.valueOf(1).equals(slot.getEnabled())) {
                return slot;
            }
        }
        return null;
    }

    public List<GameTriggerSlot> listCombatBindingsByItemIds(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return listEnabledByItemIds(itemIds).stream()
                .filter(slot -> !TriggerSlotKind.isBasicAttack(slot))
                .collect(Collectors.toList());
    }

    public List<GameTriggerSlot> listEnabledByItemIds(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.find()
                .in(GameTriggerSlot::getItemId, itemIds)
                .eq(GameTriggerSlot::getEnabled, 1)
                .orderByAsc(GameTriggerSlot::getSort)
                .list();
    }
}
