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

    /** 累计造成伤害量（阈值扳机用，会消耗） */
    private Map<String, BigDecimal> accumulatedDealDamage = new HashMap<>();

    /** 累计恢复生命量（阈值扳机用，会消耗） */
    private Map<String, BigDecimal> accumulatedHeal = new HashMap<>();

    /** 受到攻击次数（旧 HIT_COUNT） */
    private Map<String, Integer> hitCount = new HashMap<>();

    /** 累计受到伤害量（条件读取，不消耗） */
    private Map<String, BigDecimal> accumTakeDamageAmount = new HashMap<>();
    /** 累计受到伤害次数 */
    private Map<String, Integer> accumTakeDamageCount = new HashMap<>();

    /** 累计造成伤害量（条件读取，不消耗） */
    private Map<String, BigDecimal> accumDealDamageAmount = new HashMap<>();
    /** 累计造成伤害次数 */
    private Map<String, Integer> accumDealDamageCount = new HashMap<>();

    /** 累计血量增加量 / 次数 */
    private Map<String, BigDecimal> accumHpIncreaseAmount = new HashMap<>();
    private Map<String, Integer> accumHpIncreaseCount = new HashMap<>();

    /** 累计血量减少量 / 次数 */
    private Map<String, BigDecimal> accumHpDecreaseAmount = new HashMap<>();
    private Map<String, Integer> accumHpDecreaseCount = new HashMap<>();

    /**
     * 旧阈值扳机已触发次数（unitId -> slotKey -> times），
     * 避免消耗 finishedSkillCastCount 导致累计释放计数失真。
     */
    private Map<String, Map<String, Integer>> thresholdFiredCount = new HashMap<>();

    /** 装备扳机槽已触发次数 unitId -> triggerSlotId -> count */
    private Map<String, Map<String, Integer>> triggerSlotCastCount = new HashMap<>();

    /** 公式发动次数 unitId -> "finishedSkillId#formulaIndex" -> count */
    private Map<String, Map<String, Integer>> formulaCastCount = new HashMap<>();
}
