package org.wx.core.wxBusiness.game.entity.enums;

import org.wx.core.wxBusiness.game.entity.GameHeroEquip;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色装备槽位
 */
public enum HeroEquipSlot {

    WEAPON("武器", GameItemTag.WEAPON),
    ARMOR("护甲", GameItemTag.ARMOR),
    GLOVES("护手", GameItemTag.GLOVES),
    LEGS("护腿", GameItemTag.LEGS),
    HELMET("头盔", GameItemTag.HELMET),
    ACCESSORY_1("饰品1", GameItemTag.ACCESSORY),
    ACCESSORY_2("饰品2", GameItemTag.ACCESSORY),
    ACCESSORY_3("饰品3", GameItemTag.ACCESSORY);

    private final String label;
    private final GameItemTag requiredTag;

    HeroEquipSlot(String label, GameItemTag requiredTag) {
        this.label = label;
        this.requiredTag = requiredTag;
    }

    public String getLabel() {
        return label;
    }

    public GameItemTag getRequiredTag() {
        return requiredTag;
    }

    public static HeroEquipSlot parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static List<HeroEquipSlot> forTag(GameItemTag tag) {
        return Arrays.stream(values())
                .filter(slot -> slot.requiredTag == tag)
                .collect(Collectors.toList());
    }

    public String getItemId(GameHeroEquip equip) {
        if (equip == null) {
            return null;
        }
        return switch (this) {
            case WEAPON -> equip.getWeaponItemId();
            case ARMOR -> equip.getArmorItemId();
            case GLOVES -> equip.getGlovesItemId();
            case LEGS -> equip.getLegsItemId();
            case HELMET -> equip.getHelmetItemId();
            case ACCESSORY_1 -> equip.getAccessory1ItemId();
            case ACCESSORY_2 -> equip.getAccessory2ItemId();
            case ACCESSORY_3 -> equip.getAccessory3ItemId();
        };
    }

    public void setItemId(GameHeroEquip equip, String itemId) {
        if (equip == null) {
            return;
        }
        switch (this) {
            case WEAPON -> equip.setWeaponItemId(itemId);
            case ARMOR -> equip.setArmorItemId(itemId);
            case GLOVES -> equip.setGlovesItemId(itemId);
            case LEGS -> equip.setLegsItemId(itemId);
            case HELMET -> equip.setHelmetItemId(itemId);
            case ACCESSORY_1 -> equip.setAccessory1ItemId(itemId);
            case ACCESSORY_2 -> equip.setAccessory2ItemId(itemId);
            case ACCESSORY_3 -> equip.setAccessory3ItemId(itemId);
        }
    }
}
