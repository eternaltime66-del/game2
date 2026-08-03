package org.wx.core.wxBusiness.game.entity.enums;

/** 目标槽（新） */
public enum SkillTargetType {

    SELF("自己"),
    ALL_ENEMIES("全部敌方"),
    ALL_ALLIES("全部己方"),
    FRONT_ROW_ALL("前排全部"),
    MID_ROW_ALL("中排全部"),
    BACK_ROW_ALL("后排全部"),
    RANDOM_1("随机1个目标"),
    FIRST_TARGET("首位目标"),
    MAIN_TARGET("主目标"),

    /* 兼容旧数据（清库后不再使用） */
    @Deprecated RANDOM_ENEMIES("随机x个敌方"),
    @Deprecated RANDOM_ONE_ENEMY_REPEAT("随机1敌方触发x次"),
    @Deprecated CURRENT_ATTACK_TARGET("当前攻击目标"),
    @Deprecated RANDOM_ONE_ENEMY("随机1个敌方"),
    @Deprecated FRONT_ROW_RANDOM_ONE_ENEMY("前排随机1个敌方"),
    @Deprecated BACK_ROW_RANDOM_ONE_ENEMY("后排随机1个敌方"),
    @Deprecated FRONT_ROW_ENEMIES("前排敌人"),
    @Deprecated BACK_ROW_ENEMIES("后排敌人");

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

    public boolean isLegacy() {
        return this == RANDOM_ENEMIES || this == RANDOM_ONE_ENEMY_REPEAT || this == CURRENT_ATTACK_TARGET
                || this == RANDOM_ONE_ENEMY || this == FRONT_ROW_RANDOM_ONE_ENEMY || this == BACK_ROW_RANDOM_ONE_ENEMY
                || this == FRONT_ROW_ENEMIES || this == BACK_ROW_ENEMIES;
    }
}
