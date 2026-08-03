package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

/** 己方布阵 */
@Data
public class HeroFormationVo {

    private String name;

    private Integer slotCol;

    private Integer slotRow;

    private Integer footprintW;

    private Integer footprintH;

    /** 列数 */
    private Integer cols;

    /** 行数 */
    private Integer rows;
}
