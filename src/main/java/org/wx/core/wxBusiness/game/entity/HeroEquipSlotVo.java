package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class HeroEquipSlotVo {

    /** 槽位编码，如 WEAPON、ACCESSORY_1 */
    private String slot;

    /** 槽位显示名 */
    private String label;

    /** 已装备物品，空槽为 null */
    private EquippedItemVo item;

    /** 武器槽专用属性 */
    private Integer weaponAttack;

    private Integer weaponBaseActionValue;

    private java.math.BigDecimal weaponDamageRatio;
}
