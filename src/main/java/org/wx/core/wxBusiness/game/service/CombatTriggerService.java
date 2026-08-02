package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.engine.BattleEngine;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameTriggerEffectType;
import org.wx.core.wxBusiness.game.entity.enums.GameTriggerPhase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 战斗扳机 + 累计计数 → 触发完整技能
 */
@Service
public class CombatTriggerService {

    private static final int MAX_TRIGGER_DEPTH = 8;

    @Resource
    private GameItemTriggerService triggerService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    @Lazy
    private GameSkillExecutorService skillExecutorService;

    public List<BattleLog> performAttack(BattleState state, BattleUnit actor) {
        List<BattleLog> logs = new ArrayList<>();
        BattleUnit target = BattleEngine.pickTarget(state, actor);
        if (target == null) {
            logs.add(BattleLog.of(BattleLog.TYPE_ACTION, actor.getName() + " 无有效目标"));
            return logs;
        }

        List<String> actorItemIds = listEquippedItemIds(state, actor);
        List<String> targetItemIds = listEquippedItemIds(state, target);

        logs.addAll(firePhaseTriggers(state, GameTriggerPhase.BEFORE_ATTACK, actor, target, actor, actorItemIds, 0));
        if (!actor.isAlive()) {
            BattleEngine.resetActionBar(actor);
            return logs;
        }

        BigDecimal outputDamage = BattleEngine.calcDamage(actor.getAttack());
        logs.addAll(firePhaseTriggers(state, GameTriggerPhase.BEFORE_HIT, actor, target, target, targetItemIds, 0));

        int defense = target.getDefense() != null ? target.getDefense() : 0;
        BigDecimal hurt = CombatDamageService.calcReceivedDamage(outputDamage, defense);
        logs.addAll(applyDamageWithTriggers(state, target, hurt, actor, target, 0));
        logs.add(BattleLog.action(actor.getName(), target.getName(),
                CombatDamageService.formatDamage(hurt), !target.isAlive()));

        logs.addAll(firePhaseTriggers(state, GameTriggerPhase.AFTER_HIT, actor, target, target, targetItemIds, 0));
        logs.addAll(firePhaseTriggers(state, GameTriggerPhase.AFTER_ATTACK, actor, target, actor, actorItemIds, 0));

        if (BattleUnit.SIDE_HERO.equals(actor.getSide())) {
            int count = state.getHeroAttackCount() != null ? state.getHeroAttackCount() : 0;
            state.setHeroAttackCount(count + 1);
            logs.addAll(fireCounterTriggers(state, GameTriggerPhase.ATTACK_COUNT, actor, target, actor, actorItemIds, 0));
        }

        BattleEngine.resetActionBar(actor);
        return logs;
    }

    public List<BattleLog> applyDamageWithTriggers(BattleState state,
                                                     BattleUnit victim,
                                                     BigDecimal amount,
                                                     BattleUnit actor,
                                                     BattleUnit attackTarget,
                                                     int depth) {
        List<BattleLog> logs = new ArrayList<>();
        if (victim == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return logs;
        }
        if (depth > MAX_TRIGGER_DEPTH) {
            BattleEngine.applyDamage(victim, amount);
            return logs;
        }

        List<String> victimItemIds = listEquippedItemIds(state, victim);
        logs.addAll(firePhaseTriggers(state, GameTriggerPhase.BEFORE_TAKE_DAMAGE, actor, attackTarget, victim,
                victimItemIds, depth));
        if (!victim.isAlive()) {
            return logs;
        }

        BattleEngine.applyDamage(victim, amount);

        logs.addAll(firePhaseTriggers(state, GameTriggerPhase.AFTER_TAKE_DAMAGE, actor, attackTarget, victim,
                victimItemIds, depth));

        if (BattleUnit.SIDE_HERO.equals(victim.getSide())) {
            addHeroAccumulatedDamage(state, amount);
            logs.addAll(fireCounterTriggers(state, GameTriggerPhase.ACCUMULATED_TAKE_DAMAGE, actor, attackTarget,
                    victim, victimItemIds, depth));
        }

        BattleEngine.refreshAliveState(state);
        return logs;
    }

    public List<BattleLog> applySkillEffect(CombatSkillContext ctx,
                                              GameTriggerEffectType effectType,
                                              BigDecimal value,
                                              BattleUnit effectTarget) {
        if (ctx == null || effectType == null || effectTarget == null) {
            return Collections.emptyList();
        }
        return switch (effectType) {
            case DEAL_DAMAGE, TAKE_DAMAGE -> applyDamageWithTriggers(ctx.getState(), effectTarget, value,
                    ctx.getActor(), ctx.getAttackTarget(), ctx.getDepth() + 1);
            case HEAL -> applyHealWithTriggers(ctx.getState(), effectTarget, value, ctx.getActor(),
                    ctx.getAttackTarget(), ctx.getDepth() + 1);
        };
    }

    public List<BattleLog> applyHealWithTriggers(BattleState state,
                                                  BattleUnit unit,
                                                  BigDecimal amount,
                                                  BattleUnit actor,
                                                  BattleUnit attackTarget,
                                                  int depth) {
        List<BattleLog> logs = new ArrayList<>();
        if (unit == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return logs;
        }
        applyHeal(unit, amount);
        if (BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            addHeroAccumulatedHeal(state, amount);
            List<String> itemIds = listEquippedItemIds(state, unit);
            logs.addAll(fireCounterTriggers(state, GameTriggerPhase.ACCUMULATED_HEAL, actor, attackTarget, unit,
                    itemIds, depth));
        }
        return logs;
    }

    public BigDecimal normalizeValue(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(CombatDamageService.DAMAGE_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(CombatDamageService.DAMAGE_SCALE, RoundingMode.HALF_UP);
    }

    private List<BattleLog> firePhaseTriggers(BattleState state,
                                              GameTriggerPhase phase,
                                              BattleUnit actor,
                                              BattleUnit attackTarget,
                                              BattleUnit owner,
                                              List<String> ownerItemIds,
                                              int depth) {
        if (phase.isCounter() || ownerItemIds == null || ownerItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<GameItemTrigger> triggers = triggerService.listEnabledByItemIdsAndPhase(ownerItemIds, phase);
        return executeTriggers(state, triggers, phase, actor, attackTarget, owner, depth);
    }

    private List<BattleLog> fireCounterTriggers(BattleState state,
                                                GameTriggerPhase phase,
                                                BattleUnit actor,
                                                BattleUnit attackTarget,
                                                BattleUnit owner,
                                                List<String> ownerItemIds,
                                                int depth) {
        if (!phase.isCounter() || ownerItemIds == null || ownerItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<GameItemTrigger> triggers = triggerService.listEnabledByItemIdsAndPhase(ownerItemIds, phase);
        if (triggers.isEmpty()) {
            return Collections.emptyList();
        }

        List<BattleLog> logs = new ArrayList<>();
        Map<String, String> itemNameMap = loadItemNameMap(triggers);

        for (GameItemTrigger trigger : triggers) {
            BigDecimal threshold = normalizeValue(trigger.getThresholdValue());
            if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            while (shouldFireCounter(state, phase, threshold)) {
                consumeCounter(state, phase, threshold);
                logs.addAll(executeOneTrigger(state, trigger, phase, actor, attackTarget, owner, itemNameMap, depth));
                if (owner != null && !owner.isAlive()) {
                    break;
                }
            }
        }
        return logs;
    }

    private boolean shouldFireCounter(BattleState state, GameTriggerPhase phase, BigDecimal threshold) {
        return switch (phase) {
            case ACCUMULATED_TAKE_DAMAGE -> getHeroAccumulatedDamage(state).compareTo(threshold) >= 0;
            case ACCUMULATED_HEAL -> getHeroAccumulatedHeal(state).compareTo(threshold) >= 0;
            case ATTACK_COUNT -> BigDecimal.valueOf(getHeroAttackCount(state)).compareTo(threshold) >= 0;
            default -> false;
        };
    }

    private void consumeCounter(BattleState state, GameTriggerPhase phase, BigDecimal threshold) {
        switch (phase) {
            case ACCUMULATED_TAKE_DAMAGE ->
                    state.setHeroAccumulatedDamage(getHeroAccumulatedDamage(state).subtract(threshold));
            case ACCUMULATED_HEAL ->
                    state.setHeroAccumulatedHeal(getHeroAccumulatedHeal(state).subtract(threshold));
            case ATTACK_COUNT ->
                    state.setHeroAttackCount(getHeroAttackCount(state) - threshold.intValue());
            default -> {
            }
        }
    }

    private List<BattleLog> executeTriggers(BattleState state,
                                            List<GameItemTrigger> triggers,
                                            GameTriggerPhase phase,
                                            BattleUnit actor,
                                            BattleUnit attackTarget,
                                            BattleUnit owner,
                                            int depth) {
        if (triggers.isEmpty()) {
            return Collections.emptyList();
        }
        List<BattleLog> logs = new ArrayList<>();
        Map<String, String> itemNameMap = loadItemNameMap(triggers);
        for (GameItemTrigger trigger : triggers) {
            logs.addAll(executeOneTrigger(state, trigger, phase, actor, attackTarget, owner, itemNameMap, depth));
            if (owner != null && !owner.isAlive()) {
                break;
            }
            if (actor != null && !actor.isAlive()) {
                break;
            }
            if (attackTarget != null && !attackTarget.isAlive()) {
                break;
            }
        }
        return logs;
    }

    private List<BattleLog> executeOneTrigger(BattleState state,
                                                GameItemTrigger trigger,
                                                GameTriggerPhase phase,
                                                BattleUnit actor,
                                                BattleUnit attackTarget,
                                                BattleUnit owner,
                                                Map<String, String> itemNameMap,
                                                int depth) {
        if (depth > MAX_TRIGGER_DEPTH) {
            return Collections.emptyList();
        }
        String itemName = itemNameMap.getOrDefault(trigger.getItemId(), trigger.getItemId());

        if (trigger.getSkillId() != null && !trigger.getSkillId().isBlank()) {
            CombatSkillContext ctx = new CombatSkillContext();
            ctx.setState(state);
            ctx.setActor(actor);
            ctx.setAttackTarget(attackTarget);
            ctx.setOwner(owner);
            ctx.setPhase(phase);
            ctx.setSourceName(itemName);
            ctx.setDepth(depth);
            List<BattleLog> logs = new ArrayList<>(skillExecutorService.executeSkill(trigger.getSkillId(), ctx));
            logs.add(0, BattleLog.trigger("【" + itemName + "】" + phase.getLabel() + " 触发"));
            BattleEngine.refreshAliveState(state);
            return logs;
        }

        return executeLegacyTrigger(state, trigger, phase, actor, attackTarget, owner, itemName, depth);
    }

    private List<BattleLog> executeLegacyTrigger(BattleState state,
                                                 GameItemTrigger trigger,
                                                 GameTriggerPhase phase,
                                                 BattleUnit actor,
                                                 BattleUnit attackTarget,
                                                 BattleUnit owner,
                                                 String itemName,
                                                 int depth) {
        GameTriggerEffectType effectType = GameTriggerEffectType.parse(trigger.getEffectType());
        if (effectType == null || trigger.getEffectValue() == null) {
            return Collections.emptyList();
        }
        BigDecimal value = normalizeValue(trigger.getEffectValue());
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        BattleUnit effectTarget = resolveLegacyEffectTarget(effectType, phase, actor, attackTarget, owner);
        if (effectTarget == null || !effectTarget.isAlive()) {
            return Collections.emptyList();
        }

        CombatSkillContext ctx = new CombatSkillContext();
        ctx.setState(state);
        ctx.setActor(actor);
        ctx.setAttackTarget(attackTarget);
        ctx.setOwner(owner);
        ctx.setPhase(phase);
        ctx.setSourceName(itemName);
        ctx.setDepth(depth);

        List<BattleLog> logs = new ArrayList<>(applySkillEffect(ctx, effectType, value, effectTarget));
        logs.add(BattleLog.trigger(buildLegacyTriggerText(phase, effectType, itemName, owner, effectTarget, value)));
        BattleEngine.refreshAliveState(state);
        return logs;
    }

    private BattleUnit resolveLegacyEffectTarget(GameTriggerEffectType effectType,
                                                 GameTriggerPhase phase,
                                                 BattleUnit actor,
                                                 BattleUnit attackTarget,
                                                 BattleUnit owner) {
        return switch (effectType) {
            case DEAL_DAMAGE -> switch (phase) {
                case BEFORE_ATTACK, AFTER_ATTACK -> attackTarget;
                case BEFORE_HIT, AFTER_HIT, BEFORE_TAKE_DAMAGE, AFTER_TAKE_DAMAGE -> actor;
                default -> attackTarget;
            };
            case TAKE_DAMAGE, HEAL -> owner;
        };
    }

    private String buildLegacyTriggerText(GameTriggerPhase phase,
                                          GameTriggerEffectType effectType,
                                          String itemName,
                                          BattleUnit owner,
                                          BattleUnit effectTarget,
                                          BigDecimal value) {
        String amount = CombatDamageService.formatDamage(value);
        return switch (effectType) {
            case DEAL_DAMAGE -> "【" + itemName + "】" + phase.getLabel()
                    + " 对 " + effectTarget.getName() + " 造成 " + amount + " 伤害";
            case TAKE_DAMAGE -> "【" + itemName + "】" + phase.getLabel()
                    + " " + owner.getName() + " 受到 " + amount + " 伤害";
            case HEAL -> "【" + itemName + "】" + phase.getLabel()
                    + " " + owner.getName() + " 恢复 " + amount + " 生命";
        };
    }

    private void applyHeal(BattleUnit unit, BigDecimal amount) {
        BigDecimal current = unit.getHp() != null ? unit.getHp() : BigDecimal.ZERO;
        int maxHp = unit.getMaxHp() != null ? unit.getMaxHp() : 0;
        BigDecimal max = BigDecimal.valueOf(maxHp);
        BigDecimal next = current.add(amount).min(max)
                .setScale(CombatDamageService.DAMAGE_SCALE, RoundingMode.HALF_UP);
        unit.setHp(next);
        unit.setAlive(next.compareTo(BigDecimal.ZERO) > 0);
    }

    private void addHeroAccumulatedDamage(BattleState state, BigDecimal amount) {
        state.setHeroAccumulatedDamage(getHeroAccumulatedDamage(state).add(amount));
    }

    private void addHeroAccumulatedHeal(BattleState state, BigDecimal amount) {
        state.setHeroAccumulatedHeal(getHeroAccumulatedHeal(state).add(amount));
    }

    private BigDecimal getHeroAccumulatedDamage(BattleState state) {
        if (state.getHeroAccumulatedDamage() == null) {
            state.setHeroAccumulatedDamage(BigDecimal.ZERO.setScale(CombatDamageService.DAMAGE_SCALE, RoundingMode.HALF_UP));
        }
        return state.getHeroAccumulatedDamage();
    }

    private BigDecimal getHeroAccumulatedHeal(BattleState state) {
        if (state.getHeroAccumulatedHeal() == null) {
            state.setHeroAccumulatedHeal(BigDecimal.ZERO.setScale(CombatDamageService.DAMAGE_SCALE, RoundingMode.HALF_UP));
        }
        return state.getHeroAccumulatedHeal();
    }

    private int getHeroAttackCount(BattleState state) {
        return state.getHeroAttackCount() != null ? state.getHeroAttackCount() : 0;
    }

    private List<String> listEquippedItemIds(BattleState state, BattleUnit unit) {
        if (BattleUnit.SIDE_HERO.equals(unit.getSide())) {
            return state.getHeroEquippedItemIds() != null
                    ? state.getHeroEquippedItemIds()
                    : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private Map<String, String> loadItemNameMap(List<GameItemTrigger> triggers) {
        List<String> itemIds = triggers.stream()
                .map(GameItemTrigger::getItemId)
                .distinct()
                .toList();
        return gameItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(GameItem::getId, GameItem::getName, (a, b) -> a));
    }
}
