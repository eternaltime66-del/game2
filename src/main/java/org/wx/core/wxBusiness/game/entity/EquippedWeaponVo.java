package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EquippedWeaponVo {

    private String itemId;

    private String itemCode;

    private String itemName;

    private String icon;

    private Integer attack;

    private Integer baseActionValue;

    private BigDecimal damageRatio;

    /** 单次普攻期望伤害 = 总攻击 × 伤害比例 */
    private BigDecimal expectDamage;
}
