package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminTriggerSlotVo {

    private String id;

    private String itemId;

    private String itemName;

    private String triggerSlotType;

    private String triggerSlotTypeLabel;

    private BigDecimal triggerParam;

    private String triggerRefId;

    private String triggerRefName;

    private String finishedSkillId;

    private String finishedSkillName;

    /** null=无限次 */
    private Integer maxCastCount;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
