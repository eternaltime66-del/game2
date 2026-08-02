package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminCraftRecipeVo {

    private String id;

    private String code;

    private String name;

    private String resultItemId;

    private String resultItemName;

    private Integer sort;

    private Integer enabled;

    private String remark;

    private List<AdminCraftMaterialVo> materials = new ArrayList<>();
}
