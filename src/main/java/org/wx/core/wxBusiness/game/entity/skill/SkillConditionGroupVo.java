package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 条件组：组内全部为 true；多组之间 OR */
@Data
public class SkillConditionGroupVo {
    private List<SkillConditionItemVo> items = new ArrayList<>();
}
