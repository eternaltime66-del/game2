package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.math.BigDecimal;

/** 单条判定：A op B */
@Data
public class SkillConditionItemVo {
    /** READ / CONST */
    private String leftKind;
    private String leftRead;
    private String leftFilter;
    private String leftFilterRef;
    private BigDecimal leftConst;

    /** GT/LT/GTE/LTE/EQ/MOD */
    private String op;

    private String rightKind;
    private String rightRead;
    private String rightFilter;
    private String rightFilterRef;
    private BigDecimal rightConst;
}
