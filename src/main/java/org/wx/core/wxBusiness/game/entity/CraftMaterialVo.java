package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class CraftMaterialVo {

    private String itemId;

    private String itemName;

    private String icon;

    private Integer requiredQty;

    private Integer ownedQty;

    /** 缺少数量（0 表示足够） */
    private Integer missingQty;

    private Boolean enough;
}
