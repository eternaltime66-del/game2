package org.wx.core.wxBusiness.game.entity.enums;

/** 条件/公式操作数：读取 或 定值 */
public enum SkillOperandKind {
    READ,
    CONST;

    public static SkillOperandKind parse(String code) {
        if (code == null || code.isBlank()) {
            return CONST;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CONST;
        }
    }
}
