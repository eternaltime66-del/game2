package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 公式组一条：目标/频率/发动次数 + 结果类型 + 计算器 token */
@Data
public class SkillFormulaGroupVo {
    /** 目标槽（覆盖技能级） */
    private String targetType;
    private Integer targetParam;
    /** 频率槽，最小 1 */
    private Integer hitFrequency;
    /** 本公式全场发动上限，null=无限 */
    private Integer maxCastCount;

    /** DAMAGE/HEAL/ACTION_INC/... */
    private String outcome;
    private List<SkillFormulaTokenVo> tokens = new ArrayList<>();
}
