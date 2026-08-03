package org.wx.core.wxBusiness.game.entity.enums;

/** 怪物难度 / 体型分类 */
public enum MonsterRank {

    NORMAL("小怪", 1, 1),
    ELITE("精英", 2, 1),
    BOSS("Boss", 2, 2);

    private final String label;
    private final int footprintW;
    private final int footprintH;

    MonsterRank(String label, int footprintW, int footprintH) {
        this.label = label;
        this.footprintW = footprintW;
        this.footprintH = footprintH;
    }

    public String getLabel() {
        return label;
    }

    public int getFootprintW() {
        return footprintW;
    }

    public int getFootprintH() {
        return footprintH;
    }

    public static MonsterRank parse(String code) {
        if (code == null || code.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NORMAL;
        }
    }
}
