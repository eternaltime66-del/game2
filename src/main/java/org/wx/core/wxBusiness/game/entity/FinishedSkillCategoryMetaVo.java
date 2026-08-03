package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinishedSkillCategoryMetaVo {

    private List<TriggerOptionVo> catL1 = new ArrayList<>();

    private List<TriggerOptionVo> catL2 = new ArrayList<>();

    private List<TriggerOptionVo> catL4 = new ArrayList<>();
}
