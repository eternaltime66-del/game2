package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

/**
 * 前置条件：持有判定，成立返回 true。
 */
@Data
public class SkillPrerequisiteVo {

    /** HOLD_ITEM / HOLD_PERSON_SKILL / HOLD_BY_CATEGORY */
    private String type;

    /** HOLD_ITEM：物品 ID */
    private String itemId;

    /** HOLD_PERSON_SKILL：成品技能 ID（人物主动） */
    private String finishedSkillId;

    /** HOLD_BY_CATEGORY：分类过滤（空=不限该级） */
    private String catL1;
    private String catL2;
    private String catL3;
    private String catL4;
}
