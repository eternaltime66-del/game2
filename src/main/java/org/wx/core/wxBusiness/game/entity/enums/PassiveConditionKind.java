package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 被动生效条件类型 */
public enum PassiveConditionKind {

    NONE("无条件"),
    REQUIRE_EQUIP("装备某物品"),
    COMPARE("读取判定");

    private final String label;

    PassiveConditionKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PassiveConditionKind parse(String code) {
        if (code == null || code.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }

    public static List<PassiveConditionKind> all() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
