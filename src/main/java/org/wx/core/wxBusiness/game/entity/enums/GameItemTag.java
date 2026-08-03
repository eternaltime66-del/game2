package org.wx.core.wxBusiness.game.entity.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 物品标签（支持多标签，逗号分隔存储）
 */
public enum GameItemTag {

    MATERIAL("材料", 0),
    WEAPON("武器", 1),
    ARMOR("护甲", 2),
    GLOVES("护手", 3),
    LEGS("护腿", 4),
    HELMET("头盔", 5),
    ACCESSORY("饰品", 6),
    SKILL_BADGE("被动徽章", 7);

    private final String label;
    private final int sort;

    GameItemTag(String label, int sort) {
        this.label = label;
        this.sort = sort;
    }

    public String getLabel() {
        return label;
    }

    public int getSort() {
        return sort;
    }

    public static List<GameItemTag> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(MATERIAL);
        }
        Set<GameItemTag> ordered = new LinkedHashSet<>();
        for (String part : raw.split("[,，]")) {
            String code = part.trim();
            if (code.isEmpty()) {
                continue;
            }
            try {
                ordered.add(valueOf(code.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // skip unknown tag code
            }
        }
        if (ordered.isEmpty()) {
            return List.of(MATERIAL);
        }
        return ordered.stream()
                .sorted(Comparator.comparingInt(GameItemTag::getSort))
                .collect(Collectors.toList());
    }

    public static String join(List<GameItemTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return MATERIAL.name();
        }
        return tags.stream()
                .map(GameItemTag::name)
                .distinct()
                .collect(Collectors.joining(","));
    }

    public static boolean contains(String raw, GameItemTag tag) {
        if (tag == null) {
            return false;
        }
        return parseList(raw).contains(tag);
    }

    public static List<String> toLabels(String raw) {
        return parseList(raw).stream()
                .map(GameItemTag::getLabel)
                .collect(Collectors.toList());
    }

    public static List<String> toCodes(String raw) {
        return parseList(raw).stream()
                .map(GameItemTag::name)
                .collect(Collectors.toList());
    }

    /** 全部标签（按 sort 排序） */
    public static List<GameItemTag> allSorted() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(GameItemTag::getSort))
                .collect(Collectors.toList());
    }
}
