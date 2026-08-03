package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemSkillEffectDetailVo {

    private String effectKind;

    private String effectKindLabel;

    private String outcomeType;

    private String outcomeLabel;

    /** 可读公式，如「攻击力 × 1.5 × 武器伤害比例」 */
    private String formulaText;

    private String statRef;

    private String statRefLabel;

    private BigDecimal ratioY;

    private Integer useWeaponRatio;

    private BigDecimal fixedValue;

    private Integer actionDelta;

    private Integer sort;
}
