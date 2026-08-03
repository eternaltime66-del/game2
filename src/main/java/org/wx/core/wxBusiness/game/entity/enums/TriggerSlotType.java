package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** 扳机槽类型 */
public enum TriggerSlotType {

    ACTION_VALUE_PASSED("每经过x行动值", 1, true),
    ACTION_VALUE_FULL("行动值满时", 2, false),
    FINISHED_SKILL_CAST_COUNT("释放成品技能x次后", 3, true),
    ON_ATTACK("每攻击时", 4, false),
    ON_HIT_BY_ENEMY_FINISHED_SKILL("每受到敌方成品技能", 5, false),
    ON_HIT_BY_ALLY_FINISHED_SKILL("每受到己方成品技能", 6, false),
    ON_TAKE_DAMAGE("每受到伤害时", 7, false),
    ON_HEAL("每恢复生命时", 8, false),
    ACCUMULATED_DEAL_DAMAGE("每累计造成x伤害", 9, true),
    ACCUMULATED_HEAL("每累计恢复x生命", 10, true),
    HIT_COUNT("每受到x次攻击", 11, true);

    private final String label;
    private final int sort;
    private final boolean needParam;

    TriggerSlotType(String label, int sort, boolean needParam) {
        this.label = label;
        this.sort = sort;
        this.needParam = needParam;
    }

    public String getLabel() {
        return label;
    }

    public int getSort() {
        return sort;
    }

    public boolean isNeedParam() {
        return needParam;
    }

    public static TriggerSlotType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<TriggerSlotType> allSorted() {
        return Arrays.stream(values()).sorted(Comparator.comparingInt(TriggerSlotType::getSort)).collect(Collectors.toList());
    }
}
