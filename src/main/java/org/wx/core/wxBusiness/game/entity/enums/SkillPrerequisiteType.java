package org.wx.core.wxBusiness.game.entity.enums;

/** 前置条件类型 */
public enum SkillPrerequisiteType {
    /** 持有指定物品 */
    HOLD_ITEM,
    /** 持有指定人物技能（技能物品） */
    HOLD_PERSON_SKILL,
    /** 持有指定分类物品（按分类1~4过滤，未填的级忽略） */
    HOLD_BY_CATEGORY;

    public static SkillPrerequisiteType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
