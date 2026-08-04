package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.BattleState;
import org.wx.core.wxBusiness.game.entity.BattleUnit;
import org.wx.core.wxBusiness.game.entity.GameFinishedSkill;
import org.wx.core.wxBusiness.game.entity.GameInventory;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL1;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL2;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.entity.enums.SkillPrerequisiteType;
import org.wx.core.wxBusiness.game.entity.skill.SkillPrerequisiteVo;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 前置条件：持有物品 / 人物技能 / 分类物品 */
@Service
public class SkillPrerequisiteService {

    @Resource
    private GameInventoryService gameInventoryService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameTriggerSlotService triggerSlotService;
    @Resource
    private GameFinishedSkillService finishedSkillService;

    public boolean match(BattleState state, BattleUnit unit, SkillPrerequisiteVo prereq) {
        if (prereq == null) {
            return true;
        }
        SkillPrerequisiteType type = SkillPrerequisiteType.parse(prereq.getType());
        if (type == null) {
            return false;
        }
        Set<String> owned = collectOwnedItemIds(state, unit);
        return switch (type) {
            case HOLD_ITEM -> owned.contains(safe(prereq.getItemId()));
            case HOLD_PERSON_SKILL -> ownsPersonSkill(owned, prereq.getFinishedSkillId());
            case HOLD_BY_CATEGORY -> ownsByCategory(owned, prereq);
        };
    }

    private boolean ownsPersonSkill(Set<String> owned, String finishedSkillId) {
        if (finishedSkillId == null || finishedSkillId.isBlank()) {
            return false;
        }
        for (GameTriggerSlot slot : triggerSlotService.find()
                .eq(GameTriggerSlot::getFinishedSkillId, finishedSkillId)
                .list()) {
            if (slot.getItemId() != null && owned.contains(slot.getItemId())) {
                GameItem item = gameItemService.getById(slot.getItemId());
                if (item != null && GameItemTag.contains(item.getItemTags(), GameItemTag.SKILL)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean ownsByCategory(Set<String> owned, SkillPrerequisiteVo prereq) {
        if (owned.isEmpty()) {
            return false;
        }
        for (String itemId : owned) {
            if (itemMatchesCategory(itemId, prereq)) {
                return true;
            }
        }
        return false;
    }

    private boolean itemMatchesCategory(String itemId, SkillPrerequisiteVo prereq) {
        GameItem item = gameItemService.getById(itemId);
        if (item == null) {
            return false;
        }
        // 技能物品：按关联成品技能分类匹配
        if (GameItemTag.contains(item.getItemTags(), GameItemTag.SKILL)) {
            GameFinishedSkill skill = findSkillByItem(itemId);
            if (skill == null) {
                return false;
            }
            return matchSkillCats(skill, prereq);
        }
        // 普通装备/材料：分类2 对齐物品标签；分类1=EQUIP 时仅装备标签
        if (prereq.getCatL1() != null && !prereq.getCatL1().isBlank()
                && FinishedSkillCatL1.PERSON.name().equalsIgnoreCase(prereq.getCatL1().trim())) {
            return false;
        }
        if (prereq.getCatL4() != null && !prereq.getCatL4().isBlank()) {
            return false;
        }
        if (prereq.getCatL3() != null && !prereq.getCatL3().isBlank()) {
            return false;
        }
        if (prereq.getCatL2() != null && !prereq.getCatL2().isBlank()) {
            FinishedSkillCatL2 c2 = FinishedSkillCatL2.parse(prereq.getCatL2());
            try {
                GameItemTag tag = GameItemTag.valueOf(c2.name());
                return GameItemTag.contains(item.getItemTags(), tag);
            } catch (Exception ex) {
                return false;
            }
        }
        if (prereq.getCatL1() != null && !prereq.getCatL1().isBlank()
                && FinishedSkillCatL1.EQUIP.name().equalsIgnoreCase(prereq.getCatL1().trim())) {
            return hasEquipTag(item.getItemTags());
        }
        // 未填任何分类 → 任意物品都算持有
        return isBlank(prereq.getCatL1()) && isBlank(prereq.getCatL2())
                && isBlank(prereq.getCatL3()) && isBlank(prereq.getCatL4());
    }

    private boolean matchSkillCats(GameFinishedSkill skill, SkillPrerequisiteVo prereq) {
        if (!isBlank(prereq.getCatL1()) && !eqIgnore(skill.getCatL1(), prereq.getCatL1())) {
            return false;
        }
        if (!isBlank(prereq.getCatL2()) && !eqIgnore(skill.getCatL2(), prereq.getCatL2())) {
            return false;
        }
        if (!isBlank(prereq.getCatL3()) && !eqIgnore(skill.getCatL3(), prereq.getCatL3())) {
            return false;
        }
        if (!isBlank(prereq.getCatL4()) && !eqIgnore(skill.getCatL4(), prereq.getCatL4())) {
            return false;
        }
        return true;
    }

    private GameFinishedSkill findSkillByItem(String itemId) {
        List<GameTriggerSlot> slots = triggerSlotService.listByItemId(itemId);
        for (GameTriggerSlot slot : slots) {
            if (slot.getFinishedSkillId() == null || slot.getFinishedSkillId().isBlank()) {
                continue;
            }
            GameFinishedSkill skill = finishedSkillService.getById(slot.getFinishedSkillId());
            if (skill != null) {
                return skill;
            }
        }
        return null;
    }

    private Set<String> collectOwnedItemIds(BattleState state, BattleUnit unit) {
        Set<String> ids = new HashSet<>();
        if (state != null && state.getHeroEquippedItemIds() != null
                && unit != null && BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            for (String id : state.getHeroEquippedItemIds()) {
                if (id != null && !id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        if (state != null && state.getUid() != null && !state.getUid().isBlank()
                && unit != null && BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            for (GameInventory row : gameInventoryService.listOwnedRows(state.getUid())) {
                if (row.getItemId() != null && row.getQuantity() != null && row.getQuantity() > 0) {
                    ids.add(row.getItemId());
                }
            }
        }
        return ids;
    }

    private boolean hasEquipTag(String tags) {
        return GameItemTag.contains(tags, GameItemTag.WEAPON)
                || GameItemTag.contains(tags, GameItemTag.ARMOR)
                || GameItemTag.contains(tags, GameItemTag.GLOVES)
                || GameItemTag.contains(tags, GameItemTag.HELMET)
                || GameItemTag.contains(tags, GameItemTag.LEGS)
                || GameItemTag.contains(tags, GameItemTag.ACCESSORY);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean eqIgnore(String a, String b) {
        return Objects.equals(
                a == null ? null : a.trim().toUpperCase(),
                b == null ? null : b.trim().toUpperCase());
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
