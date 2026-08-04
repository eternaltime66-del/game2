package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 单条判定：左公式 op 右公式。
 * 兼容旧版 leftKind/rightKind（READ/CONST）字段，加载时迁移为 tokens。
 */
@Data
public class SkillConditionItemVo {

    /** 左公式 token（与扳机技能公式组同一套计算器） */
    private List<SkillFormulaTokenVo> leftTokens = new ArrayList<>();

    /** GT/LT/GTE/LTE/EQ/MOD(取模等于0) */
    private String op;

    /** 右公式 token */
    private List<SkillFormulaTokenVo> rightTokens = new ArrayList<>();

    /* ---- 旧字段：兼容已存档条件 ---- */
    /** @deprecated READ / CONST */
    private String leftKind;
    /** @deprecated */
    private String leftRead;
    /** @deprecated */
    private String leftFilter;
    /** @deprecated */
    private String leftFilterRef;
    /** @deprecated */
    private BigDecimal leftConst;

    /** @deprecated */
    private String rightKind;
    /** @deprecated */
    private String rightRead;
    /** @deprecated */
    private String rightFilter;
    /** @deprecated */
    private String rightFilterRef;
    /** @deprecated */
    private BigDecimal rightConst;
}
