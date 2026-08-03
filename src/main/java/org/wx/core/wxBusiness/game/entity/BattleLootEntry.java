package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class BattleLootEntry {

    private String itemId;

    private String itemCode;

    private String itemName;

    private String icon;

    private Integer quantity;
}
