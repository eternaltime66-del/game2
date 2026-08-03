package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class BattleMonsterDropItemVo {

    private String itemId;

    private String itemName;

    private String icon;

    /** 掉落概率 0-100，关卡额外掉落可为空 */
    private Integer dropRate;

    private Integer minQty;

    private Integer maxQty;
}
