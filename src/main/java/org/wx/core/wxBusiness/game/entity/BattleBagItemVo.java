package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BattleBagItemVo implements ItemTagHolder {

    private String id;

    private String itemId;

    private String itemCode;

    private String itemName;

    private String icon;

    private Integer maxStack;

    private BigDecimal unitWeight;

    private BigDecimal totalWeight;

    private Integer quantity;

    private Integer sort;

    private List<String> tags = new ArrayList<>();
}
