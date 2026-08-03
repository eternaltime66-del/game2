package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class AdminSkillBadgeVo {

    private String id;

    private String itemId;

    private String code;

    private String name;

    private String icon;

    private String passiveSkillId;

    private String passiveSkillName;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
