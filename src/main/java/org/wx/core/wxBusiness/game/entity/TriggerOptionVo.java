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

    /** 下拉分组，如 受到伤害 / 释放技能 */
    private String group;

    /** 悬停提示（如判定符号 %==0） */
    private String hint;
}
