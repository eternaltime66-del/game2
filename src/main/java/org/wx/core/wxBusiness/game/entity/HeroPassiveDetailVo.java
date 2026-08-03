package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HeroPassiveDetailVo {

    private String id;

    private String name;

    private String sourceLabel;

    private String conditionLabel;

    private String effectTypeLabel;

    private BigDecimal effectValue;

    /** 是否满足生效条件 */
    private Boolean active;
}
