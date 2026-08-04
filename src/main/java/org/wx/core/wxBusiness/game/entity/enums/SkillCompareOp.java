package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判定条件。
 * <p>
 * {@link #MOD}：left % right == 0，适合整数次数（如每 3 次普攻）。
 * {@link #EVERY}：每累计跨过一档 right 触发（⌊left/right⌋ 增加），适合小数伤害/百分比阈值。
 */
public enum SkillCompareOp {

    GT("大于", ">"),
    LT("小于", "<"),
    GTE("大于等于", ">="),
    LTE("小于等于", "<="),
    EQ("等于", "=="),
    MOD("取模等于0", "%==0"),
    /** 每累计达到：20%、40%、60%…各触发一次（不要求卡死正好等于） */
    EVERY("每累计达到", "每≥");

    private final String label;
    private final String symbol;

    SkillCompareOp(String label, String symbol) {
        this.label = label;
        this.symbol = symbol;
    }

    public String getLabel() {
        return label;
    }

    public String getSymbol() {
        return symbol;
    }

    public static SkillCompareOp parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return valueOf(code.trim().toUpperCase());
    }

    public static List<SkillCompareOp> all() {
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
