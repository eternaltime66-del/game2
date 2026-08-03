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

    private Integer enabled;

    private String remark;

    private List<AdminFinishedSkillEffectVo> effects = new ArrayList<>();
}
