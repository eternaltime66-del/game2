package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class TriggerOptionVo {

    private String code;

    private String label;

    private Integer sort;

    private Boolean needParam;

    /** 附加标记，如 FILTER / EVENT */
    private String flag;
}
