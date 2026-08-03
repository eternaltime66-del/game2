package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 公式组一条：结果类型 + 计算器 token 列表 */
@Data
public class SkillFormulaGroupVo {
    /** DAMAGE/HEAL/ACTION_INC/... */
    private String outcome;
    private List<SkillFormulaTokenVo> tokens = new ArrayList<>();
}
