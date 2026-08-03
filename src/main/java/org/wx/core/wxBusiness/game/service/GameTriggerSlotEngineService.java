package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.engine.BattleEngine;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.TriggerSlotType;

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
    @Lazy
    @Resource
    private FinishedSkillExecutorService finishedSkillExecutorService;

    public List<BattleLog> onActionValueFull(BattleState state, BattleUnit unit) {
        TriggerEventContext ctx = new TriggerEventContext();
        ctx.setActor(unit);
        return fireActionValueFull(state, unit, ctx);
    }

    private List<BattleLog> fireActionValueFull(BattleState state, BattleUnit unit, TriggerEventContext ctx) {
        List<TriggerBinding> bindings = listBindingsForUnit(state, unit).stream()
                .filter(b -> TriggerSlotType.ACTION_VALUE_FULL.name().equals(b.getTriggerSlotType()))
                .toList();
        if (bindings.isEmpty()) {
            if (BattleUnit.SIDE_HERO.equals(unit.getSide())) {
                return castFinishedSkill(state, unit, "fin_normal_attack", ctx);
            }
            return List.of();
        }
        List<TriggerBinding> toFire = bindings;
        if (BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            List<TriggerBinding> itemBindings = bindings.stream()
                    .filter(b -> b.getTriggerSlotId() != null)
                    .toList();
            if (!itemBindings.isEmpty()) {
                toFire = itemBindings;
            }
            TriggerBinding primary = toFire.get(0);
            List<BattleLog> logs = tryCastFromBinding(state, unit, primary, ctx);
            if (logs.isEmpty() && primary.getTriggerSlotId() != null) {
                for (TriggerBinding binding : bindings) {
                    if (binding.getTriggerSlotId() == null) {
                        return tryCastFromBinding(state, unit, binding, ctx);
                    }
                }
            }
            return logs;
        }
        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : toFire) {
            logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
        }
        return logs;
    }

    public void onActionValueTick(BattleState state, BattleUnit unit, int gain) {
        if (gain <= 0 || !unit.isAlive()) {
            return;
        }
        BattleTriggerCounters counters = ensureCounters(state);
        int passed = counters.getActionValuePassed().getOrDefault(unit.getUnitId(), 0) + gain;
        counters.getActionValuePassed().put(unit.getUnitId(), passed);

        TriggerEventContext ctx = new TriggerEventContext();
        ctx.setActor(unit);
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
                state.getLogs().addAll(tryCastFromBinding(state, unit, binding, ctx));
            }
        }
        counters.getActionValuePassed().put(unit.getUnitId(), passed);
    }

    public void onFinishedSkillCast(BattleState state, BattleUnit caster, String finishedSkillId, int depth) {
        BattleTriggerCounters counters = ensureCounters(state);
        counters.getFinishedSkillCastCount()
                .computeIfAbsent(caster.getUnitId(), k -> new java.util.HashMap<>())
                .merge(finishedSkillId, 1, Integer::sum);

        TriggerEventContext ctx = new TriggerEventContext();
        ctx.setActor(caster);
        ctx.setFinishedSkillId(finishedSkillId);
        ctx.setDepth(depth);

        for (TriggerBinding binding : listBindingsForUnit(state, caster)) {
            if (!TriggerSlotType.FINISHED_SKILL_CAST_COUNT.name().equals(binding.getTriggerSlotType())) {
                continue;
            }
            if (binding.getTriggerRefId() != null && !binding.getTriggerRefId().equals(finishedSkillId)) {
                continue;
            }
            fireThresholdOnce(state, caster, binding, counters, ctx);
        }
    }

    public List<BattleLog> fireInstant(BattleState state, TriggerSlotType type, BattleUnit unit, TriggerEventContext ctx) {
        return fireForUnit(state, unit, type, ctx, null);
    }

    public List<BattleLog> fireAccumulated(BattleState state, BattleUnit dealer, BattleUnit victim,
                                             TriggerEventContext ctx, BigDecimal damage, BigDecimal heal) {
        List<BattleLog> logs = new ArrayList<>();
        BattleTriggerCounters counters = ensureCounters(state);

        if (damage != null && damage.compareTo(BigDecimal.ZERO) > 0 && dealer != null) {
            BigDecimal total = counters.getAccumulatedDealDamage()
                    .merge(dealer.getUnitId(), damage, BigDecimal::add);
            logs.addAll(fireThresholdWithAmount(state, dealer, TriggerSlotType.ACCUMULATED_DEAL_DAMAGE, total, ctx));
        }
        if (heal != null && heal.compareTo(BigDecimal.ZERO) > 0 && victim != null) {
            BigDecimal total = counters.getAccumulatedHeal()
                    .merge(victim.getUnitId(), heal, BigDecimal::add);
            logs.addAll(fireThresholdWithAmount(state, victim, TriggerSlotType.ACCUMULATED_HEAL, total, ctx));
        }
        return logs;
    }

    public void recordHit(BattleUnit victim) {
        // hit count stored per battle in fireThreshold
    }

    public List<BattleLog> fireThreshold(BattleState state, TriggerSlotType type, BattleUnit unit, TriggerEventContext ctx) {
        BattleTriggerCounters counters = ensureCounters(state);
        if (type == TriggerSlotType.HIT_COUNT) {
            int count = counters.getHitCount().merge(unit.getUnitId(), 1, Integer::sum);
            return fireThresholdWithCount(state, unit, type, count, ctx);
        }
        return List.of();
    }

    private List<BattleLog> fireForUnit(BattleState state, BattleUnit unit, TriggerSlotType type,
                                          TriggerEventContext ctx, BigDecimal amount) {
        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : listBindingsForUnit(state, unit)) {
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

    private void fireThresholdOnce(BattleState state, BattleUnit unit, TriggerBinding binding,
                                     BattleTriggerCounters counters, TriggerEventContext ctx) {
        BigDecimal threshold = binding.getTriggerParam();
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int need = threshold.intValue();
        String refSkillId = binding.getTriggerRefId() != null
                ? binding.getTriggerRefId() : binding.getFinishedSkillId();
        int count = counters.getFinishedSkillCastCount()
                .getOrDefault(unit.getUnitId(), java.util.Map.of())
                .getOrDefault(refSkillId, 0);
        int remaining = count;
        while (remaining >= need) {
            remaining -= need;
            state.getLogs().addAll(tryCastFromBinding(state, unit, binding, ctx));
        }
        counters.getFinishedSkillCastCount()
                .computeIfAbsent(unit.getUnitId(), k -> new java.util.HashMap<>())
                .put(refSkillId, remaining);
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
        List<String> itemIds = listEquippedItemIds(state, unit);
        if (!itemIds.isEmpty()) {
            for (GameTriggerSlot slot : triggerSlotService.listEnabledByItemIds(itemIds)) {
                bindings.add(new TriggerBinding(
                        slot.getTriggerSlotType(), slot.getTriggerParam(), slot.getTriggerRefId(),
                        slot.getFinishedSkillId(), slot.getSort(), slot.getId(), slot.getMaxCastCount()));
            }
        }
        bindings.sort(Comparator.comparingInt(b -> b.getSort() != null ? b.getSort() : 0));
        return bindings;
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
        if (counters.getAccumulatedDealDamage() == null) {
            counters.setAccumulatedDealDamage(new java.util.HashMap<>());
        }
        if (counters.getAccumulatedHeal() == null) {
            counters.setAccumulatedHeal(new java.util.HashMap<>());
        }
        if (counters.getHitCount() == null) {
            counters.setHitCount(new java.util.HashMap<>());
        }
        return counters;
    }
}
