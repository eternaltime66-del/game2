package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminFinishedSkillVo {

    private String id;

    private String code;

    private String name;

    private String targetType;

    private String targetTypeLabel;

    private Integer targetParam;

    private String catL1;

    private String catL1Label;

    private String catL2;

    private String catL2Label;

    private String catL3;

    /** 分类3 为入口名称，与 catL3 同值 */
    private String catL3Label;

    private String catL4;

    private String catL4Label;

    private Integer enabled;

    private String remark;

    private List<AdminFinishedSkillEffectVo> effects = new ArrayList<>();
}
