package org.wx.core.wxBusiness.game.entity.enums;

/**
 * SkillReadType / StatRefType 统一解析（标签、legacy 属性映射、武器比例判定）。
 */
public final class SkillReadResolver {

    private SkillReadResolver() {
    }

    /** 读取类型或属性引用的展示标签 */
    public static String resolveLabel(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        StatRefType stat = StatRefType.parse(code);
        if (stat != null) {
            return stat.getLabel();
        }
        SkillReadType read = SkillReadType.parse(code);
        if (read != null) {
            return read.getLabel();
        }
        return code;
    }

    /** 可映射为 StatRefType 时返回，否则 null（事件读取等） */
    public static StatRefType toStatRef(String code) {
        return StatRefType.parse(code);
    }

    /** 公式/效果是否应叠加武器伤害比例 */
    public static boolean usesWeaponRatio(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        StatRefType stat = StatRefType.parse(code);
        return stat == StatRefType.ATTACK;
    }

    /** 是否可在玩家端用角色面板属性估算数值 */
    public static boolean isHeroStatPreviewable(String code) {
        StatRefType stat = StatRefType.parse(code);
        return stat == StatRefType.ATTACK
                || stat == StatRefType.DEFENSE
                || stat == StatRefType.MAX_HP
                || stat == StatRefType.WEAPON_ATTACK;
    }
}
