package org.wx.core.wxBusiness.game.entity.enums;

/**
 * 技能效果作用目标
 */
public enum GameSkillTargetType {

    SELF("自身", 1),
    ATTACK_TARGET("攻击目标", 2),
    ATTACKER("攻击者", 3);

    private final String label;
    private final int sort;

    GameSkillTargetType(String label, int sort) {
        this.label = label;
        this.sort = sort;
    }

    public String getLabel() {
        return label;
    }

    public int getSort() {
        return sort;
    }

    public static GameSkillTargetType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }
}
