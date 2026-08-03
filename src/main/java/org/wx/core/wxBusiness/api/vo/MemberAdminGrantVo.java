package org.wx.core.wxBusiness.api.vo;

import lombok.Data;

@Data
public class MemberAdminGrantVo {

    /** 用户 ID（Member.id） */
    private String id;

    private String itemId;

    private Integer quantity;
}
