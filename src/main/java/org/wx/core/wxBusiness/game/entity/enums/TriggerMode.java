package org.wx.core.wxBusiness.game.entity.enums;

import java.util.List;

/** 扳机槽模式（快捷扳机已下线，仅保留精准） */
public enum TriggerMode {
    PRECISE("精准扳机"),
    /** @deprecated 兼容旧数据；保存时一律转为 PRECISE */
    @Deprecated
    QUICK("快捷扳机");

    private final String label;

    TriggerMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TriggerMode parse(String code) {
        if (code == null || code.isBlank()) {
            return PRECISE;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PRECISE;
        }
    }

    public static List<TriggerMode> all() {
        return List.of(PRECISE);
    }
}
