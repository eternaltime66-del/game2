package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminCraftMaterialVo {

    private String id;

    private String recipeId;

    private String itemId;

    private String itemName;

    private Integer quantity;

    private Integer sort;
}
