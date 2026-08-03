package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

/** 后台-武器（武器表 + 同步物品） */
@Data
public class AdminWeaponVo {

    private String id;

    private String itemId;

    private String code;

    private String name;

    private String icon;

    private Integer maxStack;

    private Integer sort;

    private Integer attack;

    private Integer baseActionValue;

    private BigDecimal damageRatio;

    /** 是否消耗型武器 */
    private Integer consumable;

    /** 最大使用次数 */
    private Integer maxUses;

    private Integer enabled;

    private String remark;
}
