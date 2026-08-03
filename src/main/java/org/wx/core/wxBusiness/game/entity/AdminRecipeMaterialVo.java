package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class AdminRecipeMaterialVo {

    private String id;

    private String materialItemId;

    private String materialItemName;

    private Integer quantity;

    private Integer sort;
}
