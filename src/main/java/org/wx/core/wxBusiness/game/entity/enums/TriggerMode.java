package org.wx.core.wxBusiness.game.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 扳机槽模式 */
public enum TriggerMode {
    PRECISE("精准扳机"),
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
        return Arrays.stream(values()).collect(Collectors.toList());
    }
}
