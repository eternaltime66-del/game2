package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** 战斗内计数器（扳机槽用） */
@Data
public class BattleTriggerCounters {

    /** 累计经过的行动值 */
    private Map<String, Integer> actionValuePassed = new HashMap<>();

    /** 成品技能释放次数 unitId -> skillId -> count */
    private Map<String, Map<String, Integer>> finishedSkillCastCount = new HashMap<>();

    /** 成品技能被命中次数 unitId(victim) -> skillId -> count */
    private Map<String, Map<String, Integer>> finishedSkillHitCount = new HashMap<>();

    /** 累计造成伤害 */
    private Map<String, BigDecimal> accumulatedDealDamage = new HashMap<>();

    /** 累计恢复生命 */
    private Map<String, BigDecimal> accumulatedHeal = new HashMap<>();

    /** 受到攻击次数 */
    private Map<String, Integer> hitCount = new HashMap<>();

    /** 装备扳机槽已触发次数 unitId -> triggerSlotId -> count */
    private Map<String, Map<String, Integer>> triggerSlotCastCount = new HashMap<>();

    /** 公式发动次数 unitId -> "finishedSkillId#formulaIndex" -> count */
    private Map<String, Map<String, Integer>> formulaCastCount = new HashMap<>();
}
