package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 公式 token：
 * kind=READ → read/filter/filterRef
 * kind=CONST → value
 * kind=OP → op (+ - * /)
 * kind=LPAREN / RPAREN
 */
@Data
public class SkillFormulaTokenVo {
    private String kind;
    private String read;
    private String filter;
    private String filterRef;
    private BigDecimal value;
    private String op;
}
