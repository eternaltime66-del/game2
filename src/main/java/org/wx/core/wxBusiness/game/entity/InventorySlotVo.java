package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class InventorySlotVo implements ItemTagHolder {

    private Integer slotNo;

    private String itemId;

    private String itemCode;

    private String itemName;

    private String icon;

    private Integer maxStack;

    private Integer quantity;

    private BigDecimal unitWeight;

    private List<String> tags = new ArrayList<>();

    /** 物品标签编码，用于筛选 */
    private List<String> itemTagCodes = new ArrayList<>();

    private Boolean empty;
}
