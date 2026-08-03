package org.wx.core.wxBusiness.game.entity.enums;

/** 被动技能大类 */
public enum PassiveSkillKind {
    NUMERIC("数值型"),
    MECHANISM("机制型");

    private final String label;

    PassiveSkillKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PassiveSkillKind parse(String code) {
        if (code == null || code.isBlank()) {
            return NUMERIC;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NUMERIC;
        }
    }
}
