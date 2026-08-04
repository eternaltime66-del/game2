package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数值条件组：组内条目且，组间或。
 * （前置条件已提升到槽位级 {@link SkillSlotConditionsVo}，不再嵌套在组内）
 */
@Data
public class SkillConditionGroupVo {

    /** 数值比较条目（组内且） */
    private List<SkillConditionItemVo> items = new ArrayList<>();

    /** @deprecated 兼容旧 JSON：组内前置；读取后会提升到槽位级 */
    @Deprecated
    private String numericMode;

    /** @deprecated 兼容旧 JSON */
    @Deprecated
    private String prerequisiteMode;

    /** @deprecated 兼容旧 JSON */
    @Deprecated
    private List<SkillPrerequisiteVo> prerequisites = new ArrayList<>();
}
