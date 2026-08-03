package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.List;

@Data
public class PassiveSkillMetaVo {

    private List<TriggerOptionVo> conditionTypes;

    private List<TriggerOptionVo> effectTypes;

    private List<TriggerOptionVo> passiveSkillOptions;
}
