package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminMonsterPassiveVo {

    private String id;

    private String monsterId;

    private String monsterName;

    private String passiveSkillId;

    private String passiveSkillName;

    private String conditionTypeLabel;

    private String effectTypeLabel;

    private BigDecimal effectValue;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
