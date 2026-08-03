package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 技能范围过滤（累计释放/被命中等读取用） */
public enum SkillScopeFilter {

    ANY_SKILL("任意技能"),
    ANY_EQUIP_TRIGGER("任意装备扳机技能"),
    ANY_BASIC_ATTACK("任意普攻（含装备与空手）"),
    SPECIFIC_EQUIP_TRIGGER("指定装备扳机技能"),
    ANY_PERSON_TRIGGER("任意人物扳机技能"),
    SPECIFIC_PERSON_TRIGGER("指定人物扳机技能"),
    ANY_TRIGGER("任意扳机技能"),
    ANY_PERSON_SKILL("任意人物技能"),
    SPECIFIC_TRIGGER("指定扳机技能"),
    SPECIFIC_PERSON_SKILL("指定人物技能");

    private final String label;

    SkillScopeFilter(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 是否需要再选一个具体技能 ID */
    public boolean needsSkillRef() {
        return this == SPECIFIC_EQUIP_TRIGGER
                || this == SPECIFIC_PERSON_TRIGGER
                || this == SPECIFIC_TRIGGER
                || this == SPECIFIC_PERSON_SKILL;
    }

    public static SkillScopeFilter parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<SkillScopeFilter> forCast() {
        return List.of(ANY_SKILL, ANY_EQUIP_TRIGGER, ANY_BASIC_ATTACK,
                SPECIFIC_EQUIP_TRIGGER, ANY_PERSON_TRIGGER, SPECIFIC_PERSON_TRIGGER);
    }

    public static List<SkillScopeFilter> forHit() {
        return List.of(ANY_SKILL, ANY_TRIGGER, ANY_BASIC_ATTACK,
                SPECIFIC_TRIGGER, ANY_PERSON_SKILL, SPECIFIC_PERSON_SKILL);
    }

    public static List<SkillScopeFilter> all() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
