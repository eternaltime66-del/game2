package org.wx.core.wxBusiness.api.vo;

import lombok.Data;

@Data
public class MemberAdminGrantVo {

    /** 用户 ID（Member.id） */
    private String id;

    private String itemId;

    /** 人物主动技能（成品技能 ID），赠送人物技能时使用 */
    private String finishedSkillId;

    private Integer quantity;
}
