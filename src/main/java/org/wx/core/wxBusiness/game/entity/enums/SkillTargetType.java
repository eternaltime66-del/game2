package org.wx.core.wxBusiness.game.entity.enums;

/** 目标槽 */
public enum SkillTargetType {

    SELF("自己"),
    ALL_ALLIES("全部己方"),
    ALL_ENEMIES("全部敌方"),
    RANDOM_ENEMIES("随机x个敌方"),
    RANDOM_ONE_ENEMY_REPEAT("随机1敌方触发x次"),
    CURRENT_ATTACK_TARGET("当前攻击目标"),
    RANDOM_ONE_ENEMY("随机1个敌方"),
    FRONT_ROW_RANDOM_ONE_ENEMY("前排随机1个敌方"),
    BACK_ROW_RANDOM_ONE_ENEMY("后排随机1个敌方"),
    FRONT_ROW_ENEMIES("前排敌人"),
    BACK_ROW_ENEMIES("后排敌人");

    private final String label;

    SkillTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SkillTargetType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }
}
