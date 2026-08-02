package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 扳机时机
 */
public enum GameTriggerPhase {

    BEFORE_ATTACK("攻击前", 1),
    AFTER_ATTACK("攻击后", 2),
    BEFORE_HIT("受击前", 3),
    AFTER_HIT("受击后", 4),
    BEFORE_TAKE_DAMAGE("受到伤害前", 5, false),
    AFTER_TAKE_DAMAGE("受到伤害后", 6, false),
    ACCUMULATED_TAKE_DAMAGE("累计受到伤害", 7, true),
    ACCUMULATED_HEAL("累计恢复生命", 8, true),
    ATTACK_COUNT("累计攻击次数", 9, true);

    private final String label;
    private final int sort;
    private final boolean counter;

    GameTriggerPhase(String label, int sort) {
        this(label, sort, false);
    }

    GameTriggerPhase(String label, int sort, boolean counter) {
        this.label = label;
        this.sort = sort;
        this.counter = counter;
    }

    public String getLabel() {
        return label;
    }

    public int getSort() {
        return sort;
    }

    public boolean isCounter() {
        return counter;
    }

    public static GameTriggerPhase parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<GameTriggerPhase> allSorted() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(GameTriggerPhase::getSort))
                .collect(Collectors.toList());
    }
}
