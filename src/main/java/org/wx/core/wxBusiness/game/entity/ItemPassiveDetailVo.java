package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemPassiveDetailVo {

    private String id;

    private String name;

    private String conditionLabel;

    private String effectTypeLabel;

    private BigDecimal effectValue;

    private String remark;
}
