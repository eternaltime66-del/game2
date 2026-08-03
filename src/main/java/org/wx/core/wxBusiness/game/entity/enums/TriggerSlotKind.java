package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;

/** 扳机槽用途分类 */
@Getter
public enum TriggerSlotKind {

    /** 武器固定普攻槽，扳机固定为行动值满 */
    BASIC_ATTACK("普攻"),
    /** 武器固定大招槽，有且仅有一个 */
    ULTIMATE("大招"),
    /** 特性主动槽（原扳机槽） */
    TRAIT_ACTIVE("特性主动");

    private final String label;

    TriggerSlotKind(String label) {
        this.label = label;
    }

    public static TriggerSlotKind parse(String code) {
        if (code == null || code.isBlank()) {
            return TRAIT_ACTIVE;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static boolean isBasicAttack(GameTriggerSlot slot) {
        if (slot == null) {
            return false;
        }
        if (BASIC_ATTACK.name().equals(slot.getSlotKind())) {
            return true;
        }
        String kind = slot.getSlotKind();
        if (kind == null || kind.isBlank()) {
            return TriggerSlotType.ACTION_VALUE_FULL.name().equals(slot.getTriggerSlotType());
        }
        return false;
    }

    public static boolean isUltimate(GameTriggerSlot slot) {
        return slot != null && ULTIMATE.name().equals(slot.getSlotKind());
    }

    public static boolean isTraitActive(GameTriggerSlot slot) {
        if (slot == null) {
            return false;
        }
        if (isUltimate(slot) || isBasicAttack(slot)) {
            return false;
        }
        String kind = slot.getSlotKind();
        return kind == null || kind.isBlank() || TRAIT_ACTIVE.name().equals(kind);
    }
}
