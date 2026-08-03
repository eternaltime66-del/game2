package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class MaterialSourceVo {

    private String itemId;

    private String sourceType;

    private String label;

    /** 可制作时返回配方 ID */
    private String recipeId;

    /** 可出击掉落时返回推荐关卡 ID */
    private String stageId;

    /** 关卡展示编号，如 1-1 */
    private String stageDisplayCode;

    private String stageName;
}
