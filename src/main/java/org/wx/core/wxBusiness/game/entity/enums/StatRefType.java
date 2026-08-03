package org.wx.core.wxBusiness.game.entity.enums;

/** 高级效果引用的属性 */
public enum StatRefType {

    ATTACK("攻击力"),
    DEFENSE("防御力"),
    MAX_HP("生命值");

    private final String label;

    StatRefType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static StatRefType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }
}
