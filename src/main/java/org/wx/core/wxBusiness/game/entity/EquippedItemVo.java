package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EquippedItemVo {

    private String itemId;

    private String itemCode;

    private String itemName;

    private String icon;

    private List<String> tags = new ArrayList<>();
}
