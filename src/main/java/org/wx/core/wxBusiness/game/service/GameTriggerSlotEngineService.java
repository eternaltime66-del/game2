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
        if (BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            String skillId = basicAttackService.resolveSkillId(state.getHeroEquippedItemIds());
            List<BattleLog> logs = new ArrayList<>(castFinishedSkill(state, unit, skillId, ctx));
            logs.addAll(consumableWeaponService.afterBasicAttack(state));
            return logs;
        }
        if (BattleUnit.SIDE_MONSTER.equals(unit.getSide())) {
            String skillId = basicAttackService.resolveMonsterSkillId(unit.getMonsterId());
            return castFinishedSkill(state, unit, skillId, ctx);
        }

        List<TriggerBinding> bindings = listBindingsForUnit(state, unit).stream()
                .filter(b -> TriggerSlotType.ACTION_VALUE_FULL.name().equals(b.getTriggerSlotType()))
                .toList();
        if (bindings.isEmpty()) {
            return List.of();
        }
        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : bindings) {
            logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
        }
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
        logs.addAll(fireConditionDrivenOnCast(state, caster, finishedSkillId, ctx));
        return logs;
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

    private List<BattleLog> fireConditionDrivenOnCast(BattleState state, BattleUnit caster,
                                                        String finishedSkillId, TriggerEventContext ctx) {
        GameFinishedSkill castSkill = finishedSkillService.getById(finishedSkillId);
        List<BattleLog> logs = new ArrayList<>();
        for (TriggerBinding binding : listBindingsForUnit(state, caster)) {
            if (!isConditionDriven(binding)) {
                continue;
            }
            if (finishedSkillId != null && finishedSkillId.equals(binding.getFinishedSkillId())) {
                continue;
            }
            List<SkillConditionGroupVo> groups = skillJsonHelper.resolveSlotConditions(
                    binding.getTriggerMode(), binding.getQuickPreset(), binding.getConditionsJson());
            if (!conditionDependsOnCast(groups, castSkill)) {
                continue;
            }
            SkillExpressionService.SkillValueReader reader = battleReader(state, caster);
            if (skillExpressionService.anyGroupMatch(groups, reader)) {
                logs.addAll(tryCastFromBinding(state, caster, binding, ctx));
            }
        }
        return logs;
    }

    private boolean isConditionDriven(TriggerBinding binding) {
        return binding != null && binding.getTriggerMode() != null && !binding.getTriggerMode().isBlank();
    }

    private boolean conditionDependsOnCast(List<SkillConditionGroupVo> groups, GameFinishedSkill castSkill) {
        if (groups == null || castSkill == null) {
            return false;
        }
        for (SkillConditionGroupVo group : groups) {
            if (group == null || group.getItems() == null) {
                continue;
            }
            for (SkillConditionItemVo item : group.getItems()) {
                if (operandDependsOnCast(item.getLeftKind(), item.getLeftRead(), item.getLeftFilter(),
                        item.getLeftFilterRef(), castSkill)
                        || operandDependsOnCast(item.getRightKind(), item.getRightRead(), item.getRightFilter(),
                        item.getRightFilterRef(), castSkill)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean operandDependsOnCast(String kind, String read, String filter, String filterRef,
                                         GameFinishedSkill castSkill) {
        if (SkillOperandKind.parse(kind) != SkillOperandKind.READ) {
            return false;
        }
        if (SkillReadType.parse(read) != SkillReadType.ACCUM_SKILL_CAST) {
            return false;
        }
        return matchesScope(castSkill, SkillScopeFilter.parse(filter), filterRef);
    }

    private SkillExpressionService.SkillValueReader battleReader(BattleState state, BattleUnit unit) {
        SkillExpressionService.SkillValueReader base = skillExpressionService.unitReader(unit, Map.of());
        return (type, filter, filterRef) -> {
            if (type == SkillReadType.ACCUM_SKILL_CAST) {
                return BigDecimal.valueOf(sumCastCount(state, unit, filter, filterRef));
            }
            if (type == SkillReadType.ACCUM_SKILL_HIT) {
                return BigDecimal.valueOf(sumHitCount(state, unit, filter, filterRef));
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
        int remaining = count;
        List<BattleLog> logs = new ArrayList<>();
        while (remaining >= need) {
            remaining -= need;
            logs.addAll(tryCastFromBinding(state, unit, binding, ctx));
        }
        counters.getFinishedSkillCastCount()
                .computeIfAbsent(unit.getUnitId(), k -> new java.util.HashMap<>())
                .put(refSkillId, remaining);
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
        if (counters.getFormulaCastCount() == null) {
            counters.setFormulaCastCount(new java.util.HashMap<>());
        }
        return counters;
    }
}
