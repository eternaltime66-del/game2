package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminGameSkillEffectVo {

    private String id;

    private String skillId;

    private String effectType;

    private String effectTypeLabel;

    private BigDecimal effectValue;

    private String targetType;

    private String targetTypeLabel;

    private Integer sort;

    private String remark;
}
