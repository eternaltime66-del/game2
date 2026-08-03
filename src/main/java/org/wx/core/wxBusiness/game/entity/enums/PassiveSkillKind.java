package org.wx.core.wxBusiness.game.entity.enums;

/** 被动技能类型（已统一，不再区分数值/机制） */
public enum PassiveSkillKind {
    NUMERIC("被动");

    private final String label;

    PassiveSkillKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PassiveSkillKind parse(String code) {
        return NUMERIC;
    }
}
