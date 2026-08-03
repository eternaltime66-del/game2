package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminRecipeVo {

    private String id;

    private String outputItemId;

    private String outputItemName;

    /** 列表展示用，如「树叶×2、树枝×1」 */
    private String materialSummary;

    private Integer sort;

    private Integer enabled;

    private String remark;

    private List<AdminRecipeMaterialVo> materials = new ArrayList<>();
}
