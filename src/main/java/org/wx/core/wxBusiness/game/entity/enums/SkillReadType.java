package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 读取/输出值（扳机条件、公式参数、被动条件共用）。
 * 可为空表示不读取。
 */
public enum SkillReadType {

    /* ---- 单次事件输出 ---- */
    ON_TAKE_DAMAGE("受到伤害时·伤害量", 10, true, false),
    ON_HEAL("受到治疗时·治疗量", 20, true, false),
    ON_HP_INCREASE("血量增加时·增加值", 30, true, false),
    ON_HP_DECREASE("血量减少时·减少量", 40, true, false),

    /* ---- 累计输出 ---- */
    ACCUM_TAKE_DAMAGE_COUNT("累计受到伤害次数", 110, false, false),
    ACCUM_DEAL_DAMAGE_COUNT("累计造成伤害次数", 120, false, false),
    ACCUM_SKILL_CAST("累计释放技能次数", 130, false, true),
    ACCUM_SKILL_HIT("累计被技能命中次数", 140, false, true),

    /* ---- 角色/装备状态读取 ---- */
    CHAR_ATTACK("角色攻击", 210, false, false),
    CHAR_MAX_HP("角色最大生命", 220, false, false),
    CHAR_DEFENSE("角色防御", 230, false, false),
    CHAR_CUR_ACTION("角色当前行动值", 240, false, false),
    CHAR_MAX_ACTION("角色最大行动值", 250, false, false),
    EQUIP_USES_LEFT("装备剩余使用次数", 260, false, false),
    WEAPON_DAMAGE_RATIO("当前武器伤害比（无武器=1）", 270, false, false);

    private final String label;
    private final int sort;
    /** 事件瞬时输出，无事件上下文时可为空 */
    private final boolean eventScoped;
    /** 需要技能范围过滤（任意/指定扳机等） */
    private final boolean needSkillFilter;

    SkillReadType(String label, int sort, boolean eventScoped, boolean needSkillFilter) {
        this.label = label;
        this.sort = sort;
        this.eventScoped = eventScoped;
        this.needSkillFilter = needSkillFilter;
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
