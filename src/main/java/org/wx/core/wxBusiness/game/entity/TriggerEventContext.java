package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;

/** 扳机事件上下文 */
@Data
public class TriggerEventContext {

    private BattleUnit actor;

    private BattleUnit primaryTarget;

    private BattleUnit victim;

    private String finishedSkillId;

    private String finishedSkillCasterSide;

    private BigDecimal damageAmount = BigDecimal.ZERO;

    private BigDecimal healAmount = BigDecimal.ZERO;

    private int depth;
}
