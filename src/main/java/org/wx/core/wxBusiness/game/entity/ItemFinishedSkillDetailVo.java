package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemFinishedSkillDetailVo {

    private String id;

    private String code;

    private String name;

    private String targetType;

    private String targetLabel;

    private Integer targetParam;

    private String remark;

    private List<ItemSkillEffectDetailVo> effects = new ArrayList<>();
}
