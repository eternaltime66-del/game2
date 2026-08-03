package org.wx.core.wxBusiness.game.entity.enums;

/** 完整技能绑定对象 */
public enum CompleteSkillBindType {

    DEFAULT("默认"),
    HERO("主角"),
    MONSTER("怪物"),
    ITEM("物品");

    private final String label;

    CompleteSkillBindType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static CompleteSkillBindType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }
}
