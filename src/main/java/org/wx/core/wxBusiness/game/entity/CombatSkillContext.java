package org.wx.core.wxBusiness.game.entity;

import lombok.Data;
import org.wx.core.wxBusiness.game.entity.enums.GameTriggerPhase;

/**
 * 技能执行上下文
 */
@Data
public class CombatSkillContext {

    private BattleState state;

    private BattleUnit actor;

    private BattleUnit attackTarget;

    private BattleUnit owner;

    private GameTriggerPhase phase;

    private String sourceName;

    private int depth;
}
