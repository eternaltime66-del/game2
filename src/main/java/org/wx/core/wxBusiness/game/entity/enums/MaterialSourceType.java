package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

@Getter
public enum MaterialSourceType {

    CRAFT("CRAFT", "前往制作"),
    BATTLE("BATTLE", "前往出击"),
    NONE("NONE", "暂无来源");

    private final String code;
    private final String label;

    MaterialSourceType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static MaterialSourceType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (MaterialSourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
