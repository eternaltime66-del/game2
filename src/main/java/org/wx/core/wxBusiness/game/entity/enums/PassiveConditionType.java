package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

@Getter
public enum PassiveConditionType {

    NONE("无条件"),
    REQUIRE_EQUIP("需装备物品");

    private final String label;

    PassiveConditionType(String label) {
        this.label = label;
    }

    public static PassiveConditionType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }
}
