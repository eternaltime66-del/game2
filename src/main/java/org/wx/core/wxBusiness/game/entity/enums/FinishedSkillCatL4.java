package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

@Getter
public enum FinishedSkillCatL4 {

    GENERAL("通用"),
    BASIC_ATTACK("普攻"),
    ULTIMATE("大招"),
    TRAIT_ACTIVE("特性主动");

    private final String label;

    FinishedSkillCatL4(String label) {
        this.label = label;
    }

    public static FinishedSkillCatL4 parse(String code) {
        if (code == null || code.isBlank()) {
            return GENERAL;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return GENERAL;
        }
    }
}
