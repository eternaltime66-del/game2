package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL1;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL4;
import org.wx.core.wxBusiness.game.entity.enums.SkillOperandKind;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadType;
import org.wx.core.wxBusiness.game.entity.enums.SkillScopeFilter;
import org.wx.core.wxBusiness.game.entity.enums.TriggerSlotType;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionItemVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 扳机槽引擎：装备扳机槽 */
@Service
public class GameTriggerSlotEngineService {

    @Resource
    private GameTriggerSlotService triggerSlotService;
    @Resource
    private BasicAttackService basicAttackService;
    @Lazy
    @Resource
    private FinishedSkillExecutorService finishedSkillExecutorService;
    @Resource
    private ConsumableWeaponService consumableWeaponService;
    @Resource
    private SkillJsonHelper skillJsonHelper;
    @Resource
    private SkillExpressionService skillExpressionService;
    @Resource
    private GameFinishedSkillService finishedSkillService;

    public List<BattleLog> onActionValueFull(BattleState state, BattleUnit unit) {
        TriggerEventContext ctx = new TriggerEventContext();
        ctx.setActor(unit);
        return fireActionValueFull(state, unit, ctx);
    }

    private List<BattleLog> fireActionValueFull(BattleState state, BattleUnit unit, TriggerEventContext ctx) {
        List<BattleLog> logs = new ArrayList<>();
        if (BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            String skillId = basicAttackService.resolveSkillId(state.getHeroEquippedItemIds());
            logs.addAll(castFinishedSkill(state, unit, skillId, ctx));
            logs.addAll(consumableWeaponService.afterBasicAttack(state));
        } else if (BattleUnit.SIDE_MONSTER.equals(unit.getSide())) {
            String skillId = basicAttackService.resolveMonsterSkillId(unit.getMonsterId());
            logs.addAll(castFinishedSkill(state, unit, skillId, ctx));
        } else {
            List<TriggerBinding> bindings = listBindingsForUnit(state, unit).stream()
                    .filter(b -> !isConditionDriven(b))
                    .filter(b -> TriggerSlotType.ACTION_VALUE_FULL.name().equals(b.getTriggerSlotType()))
                    .toList();
            for (TriggerBinding binding : bindings) {
                logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
            }
        }
        // 能量值满时评估 V2 条件扳机（大招/人物主动等 ACTION_VALUE_FULL 预设）
        // 此时行动条尚未重置，CHAR_CUR_ACTION >= CHAR_MAX_ACTION 可成立
        logs.addAll(evaluateConditionBindings(state, unit, ctx, ConditionHook.ACTION_FULL, null, Map.of()));
        return logs;
    }

    public List<BattleLog> onActionValueTick(BattleState state, BattleUnit unit, int gain) {
        if (gain <= 0 || !unit.isAlive()) {
            return List.of();
        }
        BattleTriggerCounters counters = ensureCounters(state);
        int passed = counters.getActionValuePassed().getOrDefault(unit.getUnitId(), 0) + gain;
        counters.getActionValuePassed().put(unit.getUnitId(), passed);

        TriggerEventContext ctx = new TriggerEventContext();
        ctx.setActor(unit);
        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : listBindingsForUnit(state, unit)) {
            if (!TriggerSlotType.ACTION_VALUE_PASSED.name().equals(binding.getTriggerSlotType())) {
                continue;
            }
            BigDecimal threshold = binding.getTriggerParam();
            if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            int need = threshold.intValue();
            while (passed >= need) {
                passed -= need;
                logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
            }
        }
        counters.getActionValuePassed().put(unit.getUnitId(), passed);
        return logs;
    }

    public List<BattleLog> onFinishedSkillCast(BattleState state, BattleUnit caster, String finishedSkillId, int depth) {
        BattleTriggerCounters counters = ensureCounters(state);
        counters.getFinishedSkillCastCount()
                .computeIfAbsent(caster.getUnitId(), k -> new java.util.HashMap<>())
                .merge(finishedSkillId, 1, Integer::sum);

        TriggerEventContext ctx = new TriggerEventContext();
        ctx.setActor(caster);
        ctx.setFinishedSkillId(finishedSkillId);
        ctx.setDepth(depth);

        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : listBindingsForUnit(state, caster)) {
            if (isConditionDriven(binding)) {
                continue;
            }
            if (!TriggerSlotType.FINISHED_SKILL_CAST_COUNT.name().equals(binding.getTriggerSlotType())) {
                continue;
            }
            if (binding.getTriggerRefId() == null || binding.getTriggerRefId().isBlank()
                    || !binding.getTriggerRefId().equals(finishedSkillId)) {
                continue;
            }
            logs.addAll(fireThresholdOnce(state, caster, binding, counters, ctx));
        }
        GameFinishedSkill castSkill = finishedSkillService.getById(finishedSkillId);
        logs.addAll(evaluateConditionBindings(state, caster, ctx, ConditionHook.CAST, castSkill, Map.of()));
        return logs;
    }

    public List<BattleLog> fireInstant(BattleState state, TriggerSlotType type, BattleUnit unit, TriggerEventContext ctx) {
        return fireForUnit(state, unit, type, ctx, null);
    }

    /** 条件扳机评估时机 */
    private enum ConditionHook {
        CAST, HIT, ACTION_FULL, DAMAGE, HEAL, DEAL
    }

    public List<BattleLog> fireAccumulated(BattleState state, BattleUnit dealer, BattleUnit victim,
                                             TriggerEventContext ctx, BigDecimal damage, BigDecimal heal) {
        List<BattleLog> logs = new ArrayList<>();
        BattleTriggerCounters counters = ensureCounters(state);

        if (damage != null && damage.compareTo(BigDecimal.ZERO) > 0 && dealer != null) {
            BigDecimal total = counters.getAccumulatedDealDamage()
                    .merge(dealer.getUnitId(), damage, BigDecimal::add);
            logs.addAll(fireThresholdWithAmount(state, dealer, TriggerSlotType.ACCUMULATED_DEAL_DAMAGE, total, ctx));
            counters.getAccumDealDamageCount().merge(dealer.getUnitId(), 1, Integer::sum);
            counters.getAccumDealDamageAmount().merge(dealer.getUnitId(), damage, BigDecimal::add);
            if (victim != null) {
                counters.getAccumTakeDamageCount().merge(victim.getUnitId(), 1, Integer::sum);
                counters.getAccumTakeDamageAmount().merge(victim.getUnitId(), damage, BigDecimal::add);
                counters.getAccumHpDecreaseCount().merge(victim.getUnitId(), 1, Integer::sum);
                counters.getAccumHpDecreaseAmount().merge(victim.getUnitId(), damage, BigDecimal::add);
            }
            Map<SkillReadType, BigDecimal> victimEvents = new java.util.HashMap<>();
            victimEvents.put(SkillReadType.ON_TAKE_DAMAGE, damage);
            victimEvents.put(SkillReadType.ON_HP_DECREASE, damage);
            Map<SkillReadType, BigDecimal> dealerEvents = new java.util.HashMap<>();
            dealerEvents.put(SkillReadType.ON_DEAL_DAMAGE, damage);
            if (victim != null) {
                logs.addAll(evaluateConditionBindings(state, victim, ctx, ConditionHook.DAMAGE, null, victimEvents));
            }
            logs.addAll(evaluateConditionBindings(state, dealer, ctx, ConditionHook.DEAL, null, dealerEvents));
        }
        if (heal != null && heal.compareTo(BigDecimal.ZERO) > 0 && victim != null) {
            BigDecimal total = counters.getAccumulatedHeal()
                    .merge(victim.getUnitId(), heal, BigDecimal::add);
            logs.addAll(fireThresholdWithAmount(state, victim, TriggerSlotType.ACCUMULATED_HEAL, total, ctx));
            counters.getAccumHpIncreaseCount().merge(victim.getUnitId(), 1, Integer::sum);
            counters.getAccumHpIncreaseAmount().merge(victim.getUnitId(), heal, BigDecimal::add);
            Map<SkillReadType, BigDecimal> events = new java.util.HashMap<>();
            events.put(SkillReadType.ON_HEAL, heal);
            events.put(SkillReadType.ON_HP_INCREASE, heal);
            logs.addAll(evaluateConditionBindings(state, victim, ctx, ConditionHook.HEAL, null, events));
        }
        return logs;
    }

    /** 记录被成品技能命中，并评估「每次受到技能」等 V2 条件扳机 */
    public List<BattleLog> recordHit(BattleState state, BattleUnit victim, String finishedSkillId,
                                     TriggerEventContext ctx) {
        if (state == null || victim == null || finishedSkillId == null || finishedSkillId.isBlank()) {
            return List.of();
        }
        BattleTriggerCounters counters = ensureCounters(state);
        counters.getFinishedSkillHitCount()
                .computeIfAbsent(victim.getUnitId(), k -> new java.util.HashMap<>())
                .merge(finishedSkillId, 1, Integer::sum);
        GameFinishedSkill hitSkill = finishedSkillService.getById(finishedSkillId);
        TriggerEventContext hitCtx = ctx != null ? ctx : new TriggerEventContext();
        hitCtx.setVictim(victim);
        hitCtx.setFinishedSkillId(finishedSkillId);
        return evaluateConditionBindings(state, victim, hitCtx, ConditionHook.HIT, hitSkill, Map.of());
    }

    public List<BattleLog> fireThreshold(BattleState state, TriggerSlotType type, BattleUnit unit, TriggerEventContext ctx) {
        BattleTriggerCounters counters = ensureCounters(state);
        if (type == TriggerSlotType.HIT_COUNT) {
            int count = counters.getHitCount().merge(unit.getUnitId(), 1, Integer::sum);
            return fireThresholdWithCount(state, unit, type, count, ctx);
        }
        return List.of();
    }

    private List<BattleLog> evaluateConditionBindings(BattleState state, BattleUnit unit, TriggerEventContext ctx,
                                                        ConditionHook hook, GameFinishedSkill relatedSkill,
                                                        Map<SkillReadType, BigDecimal> eventValues) {
        if (unit == null || !unit.isAlive() || hook == null) {
            return List.of();
        }
        String skipSkillId = ctx != null ? ctx.getFinishedSkillId() : null;
        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : listBindingsForUnit(state, unit)) {
            if (!isConditionDriven(binding)) {
                continue;
            }
            if (skipSkillId != null && skipSkillId.equals(binding.getFinishedSkillId())) {
                continue;
            }
            if (relatedSkill != null && relatedSkill.getId() != null
                    && relatedSkill.getId().equals(binding.getFinishedSkillId())) {
                continue;
            }
            List<SkillConditionGroupVo> groups = skillJsonHelper.resolveSlotConditions(
                    binding.getTriggerMode(), binding.getQuickPreset(), binding.getConditionsJson());
            if (!conditionRelevantTo(groups, hook, relatedSkill)) {
                continue;
            }
            SkillExpressionService.SkillValueReader reader = battleReader(state, unit, eventValues);
            if (skillExpressionService.anyGroupMatch(groups, reader)) {
                logs.addAll(tryCastFromBinding(state, unit, binding, ctx != null ? ctx : new TriggerEventContext()));
            }
        }
        return logs;
    }

    private boolean isConditionDriven(TriggerBinding binding) {
        return binding != null && binding.getTriggerMode() != null && !binding.getTriggerMode().isBlank();
    }

    private boolean isEmptyOrAlways(List<SkillConditionGroupVo> groups) {
        if (groups == null || groups.isEmpty()) {
            return true;
        }
        for (SkillConditionGroupVo group : groups) {
            if (group == null || group.getItems() == null) {
                continue;
            }
            for (SkillConditionItemVo item : group.getItems()) {
                if (item == null) {
                    continue;
                }
                if (hasConditionContent(item)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasConditionContent(SkillConditionItemVo item) {
        if (item.getLeftTokens() != null && !item.getLeftTokens().isEmpty()) {
            return true;
        }
        if (item.getRightTokens() != null && !item.getRightTokens().isEmpty()) {
            return true;
        }
        if (item.getLeftRead() != null && !item.getLeftRead().isBlank()) {
            return true;
        }
        if (item.getRightRead() != null && !item.getRightRead().isBlank()) {
            return true;
        }
        if (item.getLeftConst() != null || item.getRightConst() != null) {
            return true;
        }
        return false;
    }

    /**
     * 仅在相关计数/事件刚变化时评估，避免：
     * - 命中计数为 0 时 0%1==0 误触发
     * - 未变化的累计条件在其它事件上重复触发
     */
    private boolean conditionRelevantTo(List<SkillConditionGroupVo> groups, ConditionHook hook,
                                        GameFinishedSkill relatedSkill) {
        if (isEmptyOrAlways(groups)) {
            return hook == ConditionHook.CAST;
        }
        boolean hasRead = false;
        boolean readRelevant = false;
        for (SkillConditionGroupVo group : groups) {
            if (group == null || group.getItems() == null) {
                continue;
            }
            for (SkillConditionItemVo item : group.getItems()) {
                SideScan left = scanSide(item.getLeftTokens(), item.getLeftKind(), item.getLeftRead(),
                        item.getLeftFilter(), item.getLeftFilterRef(), hook, relatedSkill);
                SideScan right = scanSide(item.getRightTokens(), item.getRightKind(), item.getRightRead(),
                        item.getRightFilter(), item.getRightFilterRef(), hook, relatedSkill);
                if (left.hasRead || right.hasRead) {
                    hasRead = true;
                }
                if (left.relevant || right.relevant) {
                    readRelevant = true;
                }
            }
        }
        if (!hasRead) {
            return hook == ConditionHook.CAST;
        }
        return readRelevant;
    }

    private record SideScan(boolean hasRead, boolean relevant) {}

    private SideScan scanSide(List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaTokenVo> tokens,
                              String kind, String read, String filter, String filterRef,
                              ConditionHook hook, GameFinishedSkill relatedSkill) {
        if (tokens != null && !tokens.isEmpty()) {
            boolean hasRead = false;
            boolean relevant = false;
            for (org.wx.core.wxBusiness.game.entity.skill.SkillFormulaTokenVo tk : tokens) {
                if (tk == null || tk.getKind() == null || !"READ".equalsIgnoreCase(tk.getKind())) {
                    continue;
                }
                hasRead = true;
                if (operandRelevant(SkillOperandKind.READ.name(), tk.getRead(), tk.getFilter(),
                        tk.getFilterRef(), hook, relatedSkill)) {
                    relevant = true;
                }
            }
            return new SideScan(hasRead, relevant);
        }
        if (SkillOperandKind.parse(kind) == SkillOperandKind.READ
                && read != null && !read.isBlank()) {
            return new SideScan(true, operandRelevant(kind, read, filter, filterRef, hook, relatedSkill));
        }
        return new SideScan(false, false);
    }

    private boolean operandRelevant(String kind, String read, String filter, String filterRef,
                                    ConditionHook hook, GameFinishedSkill relatedSkill) {
        if (SkillOperandKind.parse(kind) != SkillOperandKind.READ) {
            return false;
        }
        SkillReadType type = SkillReadType.parse(read);
        if (type == null) {
            return false;
        }
        return switch (type) {
            case ACCUM_SKILL_CAST -> hook == ConditionHook.CAST
                    && relatedSkill != null
                    && matchesScope(relatedSkill, SkillScopeFilter.parse(filter), filterRef);
            case ACCUM_SKILL_HIT -> hook == ConditionHook.HIT
                    && relatedSkill != null
                    && matchesScope(relatedSkill, SkillScopeFilter.parse(filter), filterRef);
            case CHAR_CUR_ACTION, CHAR_MAX_ACTION -> hook == ConditionHook.ACTION_FULL;
            case ON_TAKE_DAMAGE, ACCUM_TAKE_DAMAGE, ACCUM_TAKE_DAMAGE_COUNT,
                    ON_HP_DECREASE, ACCUM_HP_DECREASE, ACCUM_HP_DECREASE_COUNT ->
                    hook == ConditionHook.DAMAGE;
            case ON_HEAL, ON_HP_INCREASE, ACCUM_HP_INCREASE, ACCUM_HP_INCREASE_COUNT ->
                    hook == ConditionHook.HEAL;
            case ON_DEAL_DAMAGE, ACCUM_DEAL_DAMAGE, ACCUM_DEAL_DAMAGE_COUNT ->
                    hook == ConditionHook.DEAL;
            case CHAR_ATTACK, CHAR_MAX_HP, CHAR_DEFENSE, EQUIP_USES_LEFT, WEAPON_DAMAGE_RATIO ->
                    hook == ConditionHook.CAST || hook == ConditionHook.ACTION_FULL;
        };
    }

    private SkillExpressionService.SkillValueReader battleReader(BattleState state, BattleUnit unit,
                                                                   Map<SkillReadType, BigDecimal> eventValues) {
        Map<SkillReadType, BigDecimal> events = eventValues != null ? eventValues : Map.of();
        SkillExpressionService.SkillValueReader base = skillExpressionService.unitReader(unit, events);
        return (type, filter, filterRef) -> {
            BattleTriggerCounters counters = ensureCounters(state);
            String uid = unit.getUnitId();
            if (type == SkillReadType.ACCUM_SKILL_CAST) {
                return BigDecimal.valueOf(sumCastCount(state, unit, filter, filterRef));
            }
            if (type == SkillReadType.ACCUM_SKILL_HIT) {
                return BigDecimal.valueOf(sumHitCount(state, unit, filter, filterRef));
            }
            if (type == SkillReadType.ACCUM_TAKE_DAMAGE_COUNT) {
                return BigDecimal.valueOf(counters.getAccumTakeDamageCount().getOrDefault(uid, 0));
            }
            if (type == SkillReadType.ACCUM_DEAL_DAMAGE_COUNT) {
                return BigDecimal.valueOf(counters.getAccumDealDamageCount().getOrDefault(uid, 0));
            }
            if (type == SkillReadType.ACCUM_TAKE_DAMAGE) {
                return counters.getAccumTakeDamageAmount().getOrDefault(uid, BigDecimal.ZERO);
            }
            if (type == SkillReadType.ACCUM_DEAL_DAMAGE) {
                return counters.getAccumDealDamageAmount().getOrDefault(uid, BigDecimal.ZERO);
            }
            if (type == SkillReadType.ACCUM_HP_INCREASE) {
                return counters.getAccumHpIncreaseAmount().getOrDefault(uid, BigDecimal.ZERO);
            }
            if (type == SkillReadType.ACCUM_HP_INCREASE_COUNT) {
                return BigDecimal.valueOf(counters.getAccumHpIncreaseCount().getOrDefault(uid, 0));
            }
            if (type == SkillReadType.ACCUM_HP_DECREASE) {
                return counters.getAccumHpDecreaseAmount().getOrDefault(uid, BigDecimal.ZERO);
            }
            if (type == SkillReadType.ACCUM_HP_DECREASE_COUNT) {
                return BigDecimal.valueOf(counters.getAccumHpDecreaseCount().getOrDefault(uid, 0));
            }
            return base.read(type, filter, filterRef);
        };
    }

    private int sumCastCount(BattleState state, BattleUnit unit, String filter, String filterRef) {
        Map<String, Integer> casts = ensureCounters(state).getFinishedSkillCastCount()
                .getOrDefault(unit.getUnitId(), Map.of());
        SkillScopeFilter scope = SkillScopeFilter.parse(filter);
        int sum = 0;
        for (Map.Entry<String, Integer> e : casts.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) {
                continue;
            }
            GameFinishedSkill skill = finishedSkillService.getById(e.getKey());
            if (matchesScope(skill, scope, filterRef)) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    private int sumHitCount(BattleState state, BattleUnit unit, String filter, String filterRef) {
        Map<String, Integer> hits = ensureCounters(state).getFinishedSkillHitCount()
                .getOrDefault(unit.getUnitId(), Map.of());
        SkillScopeFilter scope = SkillScopeFilter.parse(filter);
        int sum = 0;
        for (Map.Entry<String, Integer> e : hits.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) {
                continue;
            }
            GameFinishedSkill skill = finishedSkillService.getById(e.getKey());
            if (matchesScope(skill, scope, filterRef)) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    boolean matchesScope(GameFinishedSkill skill, SkillScopeFilter filter, String filterRef) {
        if (skill == null) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        FinishedSkillCatL1 l1 = FinishedSkillCatL1.parse(skill.getCatL1());
        FinishedSkillCatL4 l4 = FinishedSkillCatL4.parse(skill.getCatL4());
        boolean person = l1 == FinishedSkillCatL1.PERSON
                || l1 == FinishedSkillCatL1.GENERAL
                || l1 == FinishedSkillCatL1.PROFESSION
                || l1 == FinishedSkillCatL1.CHARACTER;
        boolean equip = l1 == FinishedSkillCatL1.EQUIP;
        boolean basic = l4 == FinishedSkillCatL4.BASIC_ATTACK;
        boolean triggerLike = l4 == FinishedSkillCatL4.CUSTOM
                || l4 == FinishedSkillCatL4.ULTIMATE
                || l4 == FinishedSkillCatL4.TRAIT_ACTIVE
                || l4 == FinishedSkillCatL4.ACTIVE
                || l4 == FinishedSkillCatL4.GENERAL
                || basic;
        return switch (filter) {
            case ANY_SKILL -> true;
            case ANY_BASIC_ATTACK -> basic;
            case ANY_EQUIP_TRIGGER -> equip && !basic;
            case ANY_PERSON_TRIGGER -> person && !basic;
            case ANY_TRIGGER -> triggerLike;
            case ANY_PERSON_SKILL -> person;
            case SPECIFIC_EQUIP_TRIGGER, SPECIFIC_PERSON_TRIGGER, SPECIFIC_TRIGGER, SPECIFIC_PERSON_SKILL ->
                    filterRef != null && filterRef.equals(skill.getId());
        };
    }

    private List<BattleLog> fireForUnit(BattleState state, BattleUnit unit, TriggerSlotType type,
                                          TriggerEventContext ctx, BigDecimal amount) {
        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : listBindingsForUnit(state, unit)) {
            if (isConditionDriven(binding)) {
                continue;
            }
            TriggerSlotType slotType = TriggerSlotType.parse(binding.getTriggerSlotType());
            if (slotType != type) {
                continue;
            }
            if (type.isNeedParam()) {
                continue;
            }
            if (type == TriggerSlotType.ON_HIT_BY_ENEMY_FINISHED_SKILL
                    || type == TriggerSlotType.ON_HIT_BY_ALLY_FINISHED_SKILL) {
                if (ctx.getFinishedSkillId() == null) {
                    continue;
                }
                boolean enemyHit = TriggerSlotType.ON_HIT_BY_ENEMY_FINISHED_SKILL == type;
                boolean isEnemyCaster = ctx.getFinishedSkillCasterSide() != null
                        && !Objects.equals(ctx.getFinishedSkillCasterSide(), unit.getSide());
                if (enemyHit != isEnemyCaster) {
                    continue;
                }
            }
            logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
        }
        return logs;
    }

    private List<BattleLog> fireThresholdWithAmount(BattleState state, BattleUnit unit, TriggerSlotType type,
                                                      BigDecimal total, TriggerEventContext ctx) {
        List<BattleLog> logs = new ArrayList<>();
        BigDecimal remaining = total;
        for (TriggerBinding binding : listBindingsForUnit(state, unit)) {
            if (isConditionDriven(binding)) {
                continue;
            }
            if (!type.name().equals(binding.getTriggerSlotType())) {
                continue;
            }
            BigDecimal threshold = binding.getTriggerParam();
            if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            while (remaining.compareTo(threshold) >= 0) {
                remaining = remaining.subtract(threshold);
                logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
            }
        }
        BattleTriggerCounters counters = ensureCounters(state);
        if (type == TriggerSlotType.ACCUMULATED_DEAL_DAMAGE) {
            counters.getAccumulatedDealDamage().put(unit.getUnitId(), remaining);
        } else if (type == TriggerSlotType.ACCUMULATED_HEAL) {
            counters.getAccumulatedHeal().put(unit.getUnitId(), remaining);
        }
        return logs;
    }

    private List<BattleLog> fireThresholdWithCount(BattleState state, BattleUnit unit, TriggerSlotType type,
                                                     int count, TriggerEventContext ctx) {
        List<BattleLog> logs = new ArrayList<>();
        int remaining = count;
        for (TriggerBinding binding : listBindingsForUnit(state, unit)) {
            if (isConditionDriven(binding)) {
                continue;
            }
            if (!type.name().equals(binding.getTriggerSlotType())) {
                continue;
            }
            BigDecimal threshold = binding.getTriggerParam();
            if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            int need = threshold.intValue();
            while (remaining >= need) {
                remaining -= need;
                logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
            }
        }
        ensureCounters(state).getHitCount().put(unit.getUnitId(), remaining);
        return logs;
    }

    private List<BattleLog> fireThresholdOnce(BattleState state, BattleUnit unit, TriggerBinding binding,
                                     BattleTriggerCounters counters, TriggerEventContext ctx) {
        BigDecimal threshold = binding.getTriggerParam();
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        int need = threshold.intValue();
        String refSkillId = binding.getTriggerRefId();
        if (refSkillId == null || refSkillId.isBlank()) {
            return List.of();
        }
        int count = counters.getFinishedSkillCastCount()
                .getOrDefault(unit.getUnitId(), java.util.Map.of())
                .getOrDefault(refSkillId, 0);
        String slotKey = binding.getTriggerSlotId() != null && !binding.getTriggerSlotId().isBlank()
                ? binding.getTriggerSlotId()
                : ("ref:" + refSkillId);
        Map<String, Integer> firedMap = counters.getThresholdFiredCount()
                .computeIfAbsent(unit.getUnitId(), k -> new java.util.HashMap<>());
        int fired = firedMap.getOrDefault(slotKey, 0);
        int canFire = count / need - fired;
        if (canFire <= 0) {
            return List.of();
        }
        List<BattleLog> logs = new ArrayList<>();
        for (int i = 0; i < canFire; i++) {
            logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
            fired++;
        }
        firedMap.put(slotKey, fired);
        return logs;
    }

    private List<BattleLog> tryCastFromBinding(BattleState state, BattleUnit unit, TriggerBinding binding,
                                                 TriggerEventContext ctx) {
        if (binding.getTriggerSlotId() != null
                && binding.getMaxCastCount() != null
                && binding.getMaxCastCount() > 0) {
            BattleTriggerCounters counters = ensureCounters(state);
            Map<String, Integer> unitCounts = counters.getTriggerSlotCastCount()
                    .computeIfAbsent(unit.getUnitId(), k -> new java.util.HashMap<>());
            int used = unitCounts.getOrDefault(binding.getTriggerSlotId(), 0);
            if (used >= binding.getMaxCastCount()) {
                return List.of();
            }
            unitCounts.put(binding.getTriggerSlotId(), used + 1);
        }
        return castFinishedSkill(state, unit, binding.getFinishedSkillId(), ctx);
    }

    private List<BattleLog> castFinishedSkill(BattleState state, BattleUnit caster, String finishedSkillId,
                                                TriggerEventContext ctx) {
        if (ctx.getDepth() >= 8) {
            return List.of(BattleLog.of(BattleLog.TYPE_SKILL, "扳机链过深，已截断"));
        }
        return finishedSkillExecutorService.execute(state, caster, finishedSkillId, ctx);
    }

    private List<TriggerBinding> listBindingsForUnit(BattleState state, BattleUnit unit) {
        List<TriggerBinding> bindings = new ArrayList<>();
        if (BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            List<String> itemIds = listEquippedItemIds(state, unit);
            if (!itemIds.isEmpty()) {
                for (GameTriggerSlot slot : triggerSlotService.listCombatBindingsByItemIds(itemIds)) {
                    bindings.add(toBinding(slot));
                }
            }
        } else if (BattleUnit.SIDE_MONSTER.equals(unit.getSide()) && unit.getMonsterId() != null) {
            for (GameTriggerSlot slot : triggerSlotService.listCombatBindingsByMonsterId(unit.getMonsterId())) {
                bindings.add(toBinding(slot));
            }
        }
        bindings.sort(Comparator.comparingInt(b -> b.getSort() != null ? b.getSort() : 0));
        return bindings;
    }

    private TriggerBinding toBinding(GameTriggerSlot slot) {
        TriggerBinding binding = new TriggerBinding();
        binding.setTriggerSlotType(slot.getTriggerSlotType());
        binding.setTriggerParam(slot.getTriggerParam());
        binding.setTriggerRefId(slot.getTriggerRefId());
        binding.setFinishedSkillId(slot.getFinishedSkillId());
        binding.setSort(slot.getSort());
        binding.setTriggerSlotId(slot.getId());
        binding.setMaxCastCount(slot.getMaxCastCount());
        binding.setSourceItemId(slot.getItemId());
        binding.setSlotKind(slot.getSlotKind());
        binding.setTriggerMode(slot.getTriggerMode());
        binding.setQuickPreset(slot.getQuickPreset());
        binding.setConditionsJson(slot.getConditionsJson());
        return binding;
    }

    private List<String> listEquippedItemIds(BattleState state, BattleUnit unit) {
        if (!BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            return List.of();
        }
        if (state.getHeroEquippedItemIds() == null || state.getHeroEquippedItemIds().isEmpty()) {
            return List.of();
        }
        return state.getHeroEquippedItemIds();
    }

    private BattleTriggerCounters ensureCounters(BattleState state) {
        if (state.getTriggerCounters() == null) {
            state.setTriggerCounters(new BattleTriggerCounters());
        }
        BattleTriggerCounters counters = state.getTriggerCounters();
        if (counters.getTriggerSlotCastCount() == null) {
            counters.setTriggerSlotCastCount(new java.util.HashMap<>());
        }
        if (counters.getActionValuePassed() == null) {
            counters.setActionValuePassed(new java.util.HashMap<>());
        }
        if (counters.getFinishedSkillCastCount() == null) {
            counters.setFinishedSkillCastCount(new java.util.HashMap<>());
        }
        if (counters.getFinishedSkillHitCount() == null) {
            counters.setFinishedSkillHitCount(new java.util.HashMap<>());
        }
        if (counters.getAccumulatedDealDamage() == null) {
            counters.setAccumulatedDealDamage(new java.util.HashMap<>());
        }
        if (counters.getAccumulatedHeal() == null) {
            counters.setAccumulatedHeal(new java.util.HashMap<>());
        }
        if (counters.getHitCount() == null) {
            counters.setHitCount(new java.util.HashMap<>());
        }
        if (counters.getAccumTakeDamageCount() == null) {
            counters.setAccumTakeDamageCount(new java.util.HashMap<>());
        }
        if (counters.getAccumDealDamageCount() == null) {
            counters.setAccumDealDamageCount(new java.util.HashMap<>());
        }
        if (counters.getAccumTakeDamageAmount() == null) {
            counters.setAccumTakeDamageAmount(new java.util.HashMap<>());
        }
        if (counters.getAccumDealDamageAmount() == null) {
            counters.setAccumDealDamageAmount(new java.util.HashMap<>());
        }
        if (counters.getAccumHpIncreaseAmount() == null) {
            counters.setAccumHpIncreaseAmount(new java.util.HashMap<>());
        }
        if (counters.getAccumHpIncreaseCount() == null) {
            counters.setAccumHpIncreaseCount(new java.util.HashMap<>());
        }
        if (counters.getAccumHpDecreaseAmount() == null) {
            counters.setAccumHpDecreaseAmount(new java.util.HashMap<>());
        }
        if (counters.getAccumHpDecreaseCount() == null) {
            counters.setAccumHpDecreaseCount(new java.util.HashMap<>());
        }
        if (counters.getThresholdFiredCount() == null) {
            counters.setThresholdFiredCount(new java.util.HashMap<>());
        }
        if (counters.getFormulaCastCount() == null) {
            counters.setFormulaCastCount(new java.util.HashMap<>());
        }
        return counters;
    }
}
