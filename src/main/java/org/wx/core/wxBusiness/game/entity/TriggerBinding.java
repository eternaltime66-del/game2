package org.wx.core.wxBusiness.game.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** 扳机绑定（完整技能组 + 装备扳机槽） */
@Data
@AllArgsConstructor
public class TriggerBinding {

    private String triggerSlotType;

    private BigDecimal triggerParam;

    private String triggerRefId;

    private String finishedSkillId;

    private Integer sort;

    /** 装备扳机槽 ID，完整技能组为 null */
    private String triggerSlotId;

    /** 装备扳机槽单场释放上限，null=无限 */
    private Integer maxCastCount;

    /** 来源物品 ID（怪物扳机为 null） */
    private String sourceItemId;
}
