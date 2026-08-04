package org.wx.core.wxBusiness.game.entity.enums;

/** 伤害或治疗 */
public enum EffectOutcomeType {

    DAMAGE("伤害"),
    HEAL("治疗");

    private final String label;

    EffectOutcomeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EffectOutcomeType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
