package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件组：数值区 AND 前置条件区；组间 OR。
 * 某区 mode=NONE(不限) 时该区视为 true。
 */
@Data
public class SkillConditionGroupVo {

    /** NONE / CONFIG — 数值判定区 */
    private String numericMode;

    /** 数值比较条目（组内且） */
    private List<SkillConditionItemVo> items = new ArrayList<>();

    /** NONE / CONFIG — 前置条件区 */
    private String prerequisiteMode;

    /** 前置持有条件（组内且） */
    private List<SkillPrerequisiteVo> prerequisites = new ArrayList<>();
}
