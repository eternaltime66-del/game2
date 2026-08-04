package org.wx.core.wxBusiness.game.entity.enums;

/** 高级技能槽 - 效果种类 */
public enum AdvancedEffectKind {

    STAT_FORMULA("属性公式 x*y"),
    ACTION_VALUE("行动值增减"),
    FIXED_VALUE("固定数值");

    private final String label;

    AdvancedEffectKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AdvancedEffectKind parse(String code) {
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
