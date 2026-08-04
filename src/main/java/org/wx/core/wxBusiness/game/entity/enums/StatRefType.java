package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 属性引用（legacy 效果表 + 公式预览） */
public enum StatRefType {

    ATTACK("攻击力"),
    DEFENSE("防御力"),
    MAX_HP("生命值"),
    CUR_HP("当前生命值"),
    MAX_ACTION("最大行动值"),
    CUR_ACTION("当前行动值"),
    WEAPON_ATTACK("武器攻击力"),
    WEAPON_DAMAGE_RATIO("武器伤害比例");

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
        String normalized = normalizeCode(code.trim().toUpperCase());
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** CHAR_ATTACK 等 SkillReadType 别名 → StatRefType */
    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        return switch (code.trim().toUpperCase()) {
            case "CHAR_ATTACK" -> ATTACK.name();
            case "CHAR_DEFENSE" -> DEFENSE.name();
            case "CHAR_MAX_HP", "HP" -> MAX_HP.name();
            case "CHAR_CUR_HP" -> CUR_HP.name();
            case "CHAR_MAX_ACTION" -> MAX_ACTION.name();
            case "CHAR_CUR_ACTION" -> CUR_ACTION.name();
            default -> code.trim().toUpperCase();
        };
    }

    public static List<StatRefType> all() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
