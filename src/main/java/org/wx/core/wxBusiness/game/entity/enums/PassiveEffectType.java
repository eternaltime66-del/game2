package org.wx.core.wxBusiness.game.entity.enums;

import lombok.Getter;

@Getter
public enum PassiveEffectType {

    ATTACK_FLAT("攻击力 +x"),
    DEFENSE_FLAT("防御力 +x"),
    HP_FLAT("生命值 +x"),
    ATTACK_PCT("攻击力额外 +y%"),
    DEFENSE_PCT("防御力额外 +y%"),
    HP_PCT("生命值额外 +y%"),
    ACTION_VALUE_REDUCE_PCT("最终行动值 -y%");

    private final String label;

    PassiveEffectType(String label) {
        this.label = label;
    }

    public static PassiveEffectType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isPercent() {
        return this == ATTACK_PCT || this == DEFENSE_PCT || this == HP_PCT || this == ACTION_VALUE_REDUCE_PCT;
    }
}
