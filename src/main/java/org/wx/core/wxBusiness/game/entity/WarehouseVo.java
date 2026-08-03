package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WarehouseVo {

    private Integer maxSlots;

    private Integer usedSlots;

    private List<InventorySlotVo> slots = new ArrayList<>();

    /** 仓库内可用筛选项（按物品标签动态生成） */
    private List<ItemTagFilterVo> tagFilters = new ArrayList<>();
}
