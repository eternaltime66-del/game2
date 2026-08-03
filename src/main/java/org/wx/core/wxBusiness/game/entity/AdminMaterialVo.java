package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

/** 后台-材料（纯 app_game_item） */
@Data
public class AdminMaterialVo {

    private String id;

    private String code;

    private String name;

    private String icon;

    private Integer maxStack;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
