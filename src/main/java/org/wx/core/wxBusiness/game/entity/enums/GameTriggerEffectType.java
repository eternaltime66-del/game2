package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 扳机效果类型
 */
public enum GameTriggerEffectType {

    DEAL_DAMAGE("造成伤害", 1),
    TAKE_DAMAGE("受到伤害", 2),
    HEAL("受到治疗", 3);

    private final String label;
    private final int sort;

    GameTriggerEffectType(String label, int sort) {
        this.label = label;
        this.sort = sort;
    }

    public String getLabel() {
        return label;
    }

    public int getSort() {
        return sort;
    }

    public static GameTriggerEffectType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<GameTriggerEffectType> allSorted() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(GameTriggerEffectType::getSort))
                .collect(Collectors.toList());
    }
}
