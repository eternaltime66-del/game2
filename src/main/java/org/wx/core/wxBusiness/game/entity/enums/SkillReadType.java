package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 读取/输出值（扳机条件、公式参数、被动条件共用）。
 */
public enum SkillReadType {

    /* ---- 受到伤害 ---- */
    ON_TAKE_DAMAGE("单次受到伤害量", 10, true, false, "受到伤害"),
    ACCUM_TAKE_DAMAGE("累计受到伤害量", 11, false, false, "受到伤害"),
    ACCUM_TAKE_DAMAGE_COUNT("受到伤害次数", 12, false, false, "受到伤害"),

    /* ---- 造成伤害 ---- */
    ON_DEAL_DAMAGE("单次造成伤害量", 20, true, false, "造成伤害"),
    ACCUM_DEAL_DAMAGE("累计造成伤害量", 21, false, false, "造成伤害"),
    ACCUM_DEAL_DAMAGE_COUNT("造成伤害次数", 22, false, false, "造成伤害"),

    /* ---- 血量增加 ---- */
    ON_HP_INCREASE("单次血量增加量", 30, true, false, "血量增加"),
    ACCUM_HP_INCREASE("累计血量增加量", 31, false, false, "血量增加"),
    ACCUM_HP_INCREASE_COUNT("血量增加次数", 32, false, false, "血量增加"),

    /* ---- 血量减少 ---- */
    ON_HP_DECREASE("单次血量减少量", 40, true, false, "血量减少"),
    ACCUM_HP_DECREASE("累计血量减少量", 41, false, false, "血量减少"),
    ACCUM_HP_DECREASE_COUNT("血量减少次数", 42, false, false, "血量减少"),

    /* ---- 受到治疗 ---- */
    ON_HEAL("单次受到治疗量", 50, true, false, "受到治疗"),

    /* ---- 释放 / 受到技能 ---- */
    ACCUM_SKILL_CAST("累计释放技能次数", 130, false, true, "释放技能"),
    ACCUM_SKILL_HIT("累计被技能命中次数", 140, false, true, "受到技能"),

    /* ---- 角色属性 ---- */
    CHAR_MAX_HP("角色最大血量", 210, false, false, "角色属性"),
    CHAR_CUR_HP("角色当前血量", 211, false, false, "角色属性"),
    CHAR_ATTACK("角色攻击力", 220, false, false, "角色属性"),
    CHAR_DEFENSE("角色防御力", 230, false, false, "角色属性"),
    CHAR_MAX_ACTION("角色最大行动值", 240, false, false, "角色属性"),
    CHAR_CUR_ACTION("角色当前行动值", 250, false, false, "角色属性"),

    /* ---- 武器 ---- */
    WEAPON_ATTACK("武器攻击力", 310, false, false, "武器"),
    WEAPON_DAMAGE_RATIO("武器伤害比例", 320, false, false, "武器"),
    /** 兼容旧名 EQUIP_USES_LEFT */
    EQUIP_USES_LEFT("武器剩余使用次数", 330, false, false, "武器");

    private final String label;
    private final int sort;
    private final boolean eventScoped;
    private final boolean needSkillFilter;
    private final String group;

    SkillReadType(String label, int sort, boolean eventScoped, boolean needSkillFilter, String group) {
        this.label = label;
        this.sort = sort;
        this.eventScoped = eventScoped;
        this.needSkillFilter = needSkillFilter;
        this.group = group;
    }

    public String getLabel() {
        return label;
    }

    public int getSort() {
        return sort;
    }

    public boolean isEventScoped() {
        return eventScoped;
    }

    public boolean isNeedSkillFilter() {
        return needSkillFilter;
    }

    public String getGroup() {
        return group;
    }

    public static SkillReadType parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<SkillReadType> allSorted() {
        return Arrays.stream(values()).sorted(Comparator.comparingInt(SkillReadType::getSort)).collect(Collectors.toList());
    }
}
