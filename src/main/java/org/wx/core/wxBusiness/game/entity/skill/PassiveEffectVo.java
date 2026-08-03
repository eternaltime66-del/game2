package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 被动效果一条 */
@Data
public class PassiveEffectVo {
    private String targetType;
    /** PassiveEffectKind */
    private String kind;
    /** ATTACK / DEFENSE / HP */
    private String stat;
    /** +1 / -1 */
    private Integer sign;
    private BigDecimal value;
    private List<SkillFormulaTokenVo> tokens = new ArrayList<>();
}
