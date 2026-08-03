package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminPassiveSkillVo {

    private String id;

    private String code;

    private String name;

    private String conditionType;

    private String conditionTypeLabel;

    private String conditionEquipItemId;

    private String conditionEquipItemName;

    private String effectType;

    private String effectTypeLabel;

    private BigDecimal effectValue;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
