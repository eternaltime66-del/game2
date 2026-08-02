package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class MonsterDropPreviewVo {

    private String itemId;

    private String itemCode;

    private String itemName;

    private String icon;

    /** 掉落概率 0-100 */
    private Integer dropRate;

    private Integer minQty;

    private Integer maxQty;
}
