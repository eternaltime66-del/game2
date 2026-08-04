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

    /** NONE / CONFIG — 前置条件区（与数值区同级） */
    private String prerequisiteMode;

    private List<org.wx.core.wxBusiness.game.entity.skill.SkillPrerequisiteVo> prerequisites = new ArrayList<>();

    /** NONE / CONFIG — 数值判定区 */
    private String numericMode;

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
