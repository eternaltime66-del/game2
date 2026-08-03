package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

@Getter
public enum FinishedSkillCatL1 {

    EQUIP("装备"),
    PERSON("人物"),
    MONSTER("怪物"),
    /** @deprecated 兼容旧数据，新数据用 PERSON */
    GENERAL("通用"),
    /** @deprecated 兼容旧数据，新数据用 PERSON + L2 */
    PROFESSION("职业"),
    /** @deprecated 兼容旧数据，新数据用 PERSON + L2 */
    CHARACTER("角色");

    private final String label;

    FinishedSkillCatL1(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static FinishedSkillCatL1 parse(String code) {
        if (code == null || code.isBlank()) {
            return EQUIP;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EQUIP;
        }
    }
}
