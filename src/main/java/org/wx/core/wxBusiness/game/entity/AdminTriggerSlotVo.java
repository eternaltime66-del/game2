package org.wx.core.wxBusiness.game.entity;

import lombok.Data;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminTriggerSlotVo {

    private String id;

    private String itemId;

    private String itemName;

    private String monsterId;

    private String monsterName;

    private String slotKind;

    private String slotKindLabel;

    /** PRECISE / QUICK */
    private String triggerMode;

    private String triggerModeLabel;

    private String quickPreset;

    private String quickPresetLabel;

    private List<SkillConditionGroupVo> conditionGroups = new ArrayList<>();

    /** @deprecated */
    private String triggerSlotType;

    private String triggerSlotTypeLabel;

    private BigDecimal triggerParam;

    private String triggerRefId;

    private String triggerRefName;

    private String finishedSkillId;

    private String finishedSkillName;

    /** @deprecated 上限在成品技能 */
    private Integer maxCastCount;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
