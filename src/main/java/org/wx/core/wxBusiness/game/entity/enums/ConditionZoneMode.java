package org.wx.core.wxBusiness.game.entity.enums;

/** 条件组分区模式：不限 / 配置 */
public enum ConditionZoneMode {
    NONE,
    CONFIG;

    public static ConditionZoneMode parse(String code) {
        if (code == null || code.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
