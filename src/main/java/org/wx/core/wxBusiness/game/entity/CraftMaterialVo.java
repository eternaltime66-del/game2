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

    /** 获取方式：BATTLE / CRAFT / NONE */
    private String sourceType;

    /** 展示文案：掉落 / 去合成 / 敬请期待 */
    private String sourceLabel;

    /** 可合成时返回配方 ID */
    private String sourceRecipeId;
}
