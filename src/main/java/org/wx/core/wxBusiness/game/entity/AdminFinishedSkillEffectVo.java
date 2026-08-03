package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminFinishedSkillEffectVo {

    private String id;

    private String finishedSkillId;

    private String effectKind;

    private String effectKindLabel;

    private String outcomeType;

    private String outcomeTypeLabel;

    private String statRef;

    private String statRefLabel;

    private BigDecimal ratioY;

    private Integer useWeaponRatio;

    private BigDecimal ratioZ;

    private BigDecimal fixedValue;

    private Integer actionDelta;

    private Integer sort;

    private String remark;
}
