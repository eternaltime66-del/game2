package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class GameStageSelectVo {

    private String id;

    private String name;

    private String displayCode;

    private Integer groupNo;

    private Integer stageNo;

    private Integer waveCount;
}
