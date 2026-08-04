package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 扳机槽条件：前置区 与 数值区 同级，两区均为 true 才命中。
 * 某区 mode=NONE(不限) 时该区视为 true。
 */
@Data
public class SkillSlotConditionsVo {

    /** NONE / CONFIG — 前置条件区 */
    private String prerequisiteMode;

    /** 前置持有条件（区内且） */
    private List<SkillPrerequisiteVo> prerequisites = new ArrayList<>();

    /** NONE / CONFIG — 数值判定区 */
    private String numericMode;

    /** 数值条件（全部且；存储仍为一组 items，兼容旧多组数据拍平） */
    private List<SkillConditionGroupVo> conditionGroups = new ArrayList<>();
}
