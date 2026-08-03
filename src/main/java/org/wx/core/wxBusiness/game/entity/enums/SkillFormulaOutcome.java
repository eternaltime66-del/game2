package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 公式/效果结果类型（扳机技能公式组） */
public enum SkillFormulaOutcome {

    DAMAGE("伤害"),
    HEAL("治疗"),
    ACTION_INC("当前行动值增加"),
    ACTION_DEC("当前行动值减少"),
    ENERGY_MAX_INC("最大能量值增加"),
    ENERGY_MAX_DEC("最大能量值减少");

    private final String label;

    SkillFormulaOutcome(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SkillFormulaOutcome parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<SkillFormulaOutcome> all() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
