package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HeroStatBreakdownRowVo {

    private String label;

    /** 角色基础值 */
    private Integer base;

    /** 装备固定加成（武器攻击、饰品攻击等） */
    private Integer equipBonus;

    /** 被动固定加成 */
    private Integer passiveFlat;

    /** 被动百分比乘数，如 1.05 表示 +5% */
    private BigDecimal pctMultiplier;

    /** 最终值 */
    private Integer total;
}
