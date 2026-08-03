package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 判定条件：大于/小于/…/取模等于0（left % right == 0） */
public enum SkillCompareOp {

    GT("大于", ">"),
    LT("小于", "<"),
    GTE("大于等于", ">="),
    LTE("小于等于", "<="),
    EQ("等于", "=="),
    MOD("取模等于0", "%==0");

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
