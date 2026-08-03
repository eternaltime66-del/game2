package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

/** 后台-防具（防具表 + 同步物品） */
@Data
public class AdminArmorVo {

    private String id;

    private String itemId;

    private String code;

    private String name;

    private String icon;

    private Integer maxStack;

    private Integer sort;

    private Integer bonusHp;

    private Integer defense;

    private Integer bonusAttack;

    private Integer enabled;

    private String remark;
}
