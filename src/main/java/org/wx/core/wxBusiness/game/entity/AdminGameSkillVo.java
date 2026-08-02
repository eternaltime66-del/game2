package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminGameSkillVo {

    private String id;

    private String code;

    private String name;

    private Integer sort;

    private Integer enabled;

    private String remark;

    private List<AdminGameSkillEffectVo> effects = new ArrayList<>();
}
