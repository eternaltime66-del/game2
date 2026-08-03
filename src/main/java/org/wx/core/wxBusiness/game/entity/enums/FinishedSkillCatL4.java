package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

/**
 * 分类4：技能类型（自定义/主动/被动）+ 兼容旧槽位语义（普攻/大招/特性主动）
 */
@Getter
public enum FinishedSkillCatL4 {

    CUSTOM("自定义"),
    ACTIVE("主动"),
    PASSIVE("被动"),
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
            return CUSTOM;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CUSTOM;
        }
    }
}
