package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminCompleteSkillVo {

    private String id;

    private String code;

    private String name;

    private String triggerSlotType;

    private String triggerSlotTypeLabel;

    private BigDecimal triggerParam;

    private String triggerRefId;

    private String triggerRefName;

    private String finishedSkillId;

    private String finishedSkillName;

    private String bindType;

    private String bindTypeLabel;

    private String bindRefId;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
