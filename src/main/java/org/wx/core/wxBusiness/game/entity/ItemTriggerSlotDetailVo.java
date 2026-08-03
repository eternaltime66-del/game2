package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemTriggerSlotDetailVo {

    private String id;

    private String triggerSlotType;

    private String triggerSlotTypeLabel;

    /** 扳机条件描述，如「行动值满时」「每经过 100 行动值」 */
    private String triggerDesc;

    private BigDecimal triggerParam;

    private String triggerRefId;

    private String triggerRefName;

    /** null 或 ≤0 表示无限 */
    private Integer maxCastCount;

    private String castLimitText;

    private ItemFinishedSkillDetailVo finishedSkill;
}
