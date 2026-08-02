package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CraftRecipeVo {

    private String id;

    private String code;

    private String name;

    private String remark;

    private String resultItemId;

    private String resultItemName;

    private String resultItemIcon;

    private Integer armorBonusHp;

    private Integer armorDefense;

    private List<CraftMaterialVo> materials = new ArrayList<>();

    /** 仓库中不足的材料 */
    private List<CraftMaterialVo> missingMaterials = new ArrayList<>();

    private Boolean canCraft;
}
