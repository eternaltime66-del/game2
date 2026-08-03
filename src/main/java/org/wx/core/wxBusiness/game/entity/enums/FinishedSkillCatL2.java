package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

@Getter
public enum FinishedSkillCatL2 {

    GENERAL("通用"),
    WEAPON("武器"),
    ARMOR("护甲"),
    GLOVES("护手"),
    HELMET("头盔"),
    LEGS("护腿"),
    ACCESSORY("饰品"),
    CHARACTER("角色"),
    PROFESSION("职业"),
    MONSTER("怪物");

    private final String label;

    FinishedSkillCatL2(String label) {
        this.label = label;
    }

    public static FinishedSkillCatL2 parse(String code) {
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
