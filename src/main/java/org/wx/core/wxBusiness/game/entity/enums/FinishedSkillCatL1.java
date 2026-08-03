package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

@Getter
public enum FinishedSkillCatL1 {

    GENERAL("通用"),
    EQUIP("装备"),
    MONSTER("怪物");

    private final String label;

    FinishedSkillCatL1(String label) {
        this.label = label;
    }

    public static FinishedSkillCatL1 parse(String code) {
        if (code == null || code.isBlank()) {
            return GENERAL;
        }
        return valueOf(code.trim().toUpperCase());
    }
}
