package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class ItemDropSourceVo {

    private String stageId;

    private String displayCode;

    private String stageName;

    private String monsterId;

    private String monsterName;

    private Integer dropRate;

    private Integer minQty;

    private Integer maxQty;
}
