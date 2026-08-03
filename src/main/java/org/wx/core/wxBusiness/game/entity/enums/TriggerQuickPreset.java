package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 快捷扳机预设（展开为精准条件组）。
 * ON_HIT_BY_SKILL ≈ 累计被任意扳机技能命中次数 % 1 == 0
 * ON_CAST_SKILL ≈ 累计释放任意扳机技能次数 % 1 == 0
 */
public enum TriggerQuickPreset {

    ON_HIT_BY_SKILL("每次受到技能", SkillReadType.ACCUM_SKILL_HIT, SkillScopeFilter.ANY_TRIGGER),
    ON_CAST_SKILL("每释放技能", SkillReadType.ACCUM_SKILL_CAST, SkillScopeFilter.ANY_SKILL),
    ACTION_VALUE_FULL("能量值满", SkillReadType.CHAR_CUR_ACTION, null);

    private final String label;
    private final SkillReadType readType;
    private final SkillScopeFilter scopeFilter;

    TriggerQuickPreset(String label, SkillReadType readType, SkillScopeFilter scopeFilter) {
        this.label = label;
        this.readType = readType;
        this.scopeFilter = scopeFilter;
    }

    public String getLabel() {
        return label;
    }

    public SkillReadType getReadType() {
        return readType;
    }

    public SkillScopeFilter getScopeFilter() {
        return scopeFilter;
    }

    public static TriggerQuickPreset parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<TriggerQuickPreset> all() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
