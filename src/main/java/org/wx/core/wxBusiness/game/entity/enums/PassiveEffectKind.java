package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 被动效果类型（每条效果选一个） */
public enum PassiveEffectKind {

    OUT_STAT_FLAT("战斗外·属性定值", true, false),
    OUT_STAT_MULT("战斗外·属性最终叠乘%", true, false),
    OUT_MAX_ENERGY_FLAT("战斗外·最大能量定值", false, true),
    OUT_MAX_ENERGY_MULT("战斗外·最大能量叠乘%", false, true),
    IN_ENERGY_FLAT("战斗内·当前能量定值", false, true),
    IN_BASE_STAT_FLAT("战斗内·基础属性定值", true, false),
    IN_STAT_MULT("战斗内·属性最终叠乘%", true, false),
    IN_MAX_ENERGY_FLAT("战斗内·最大能量定值", false, true),
    IN_MAX_ENERGY_MULT("战斗内·最大能量叠乘%", false, true),
    FORMULA_DAMAGE("公式·伤害", false, false),
    FORMULA_HEAL("公式·治疗", false, false);

    private final String label;
    private final boolean needStat;
    private final boolean energyRelated;

    PassiveEffectKind(String label, boolean needStat, boolean energyRelated) {
        this.label = label;
        this.needStat = needStat;
        this.energyRelated = energyRelated;
    }

    public String getLabel() {
        return label;
    }

    public boolean isNeedStat() {
        return needStat;
    }

    public boolean isEnergyRelated() {
        return energyRelated;
    }

    public boolean isOutBattle() {
        return name().startsWith("OUT_");
    }

    public boolean isFormula() {
        return name().startsWith("FORMULA_");
    }

    public static PassiveEffectKind parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<PassiveEffectKind> all() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
