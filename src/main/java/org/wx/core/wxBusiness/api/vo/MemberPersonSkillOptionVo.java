package org.wx.core.wxBusiness.api.vo;

import lombok.Data;

@Data
public class MemberPersonSkillOptionVo {

    /** 成品技能 ID */
    private String id;

    private String code;

    private String name;

    /** 可进仓库/技能槽的技能物品 ID */
    private String skillItemId;

    private String catL2;

    private String catL2Label;
}
