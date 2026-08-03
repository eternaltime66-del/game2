package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.engine.BattleEngine;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** 成品技能执行器 */
@Service
public class FinishedSkillExecutorService {

    private static final int MAX_DEPTH = 8;

    @Resource
    private GameFinishedSkillService finishedSkillService;
    @Resource
    private GameFinishedSkillEffectService finishedSkillEffectService;
    @Resource
    private GameTriggerSlotEngineService triggerSlotEngineService;
    @Resource
    private GameWeaponService gameWeaponService;

    public List<BattleLog> execute(BattleState state, BattleUnit caster, String finishedSkillId, TriggerEventContext ctx) {
        if (ctx == null) {
            ctx = new TriggerEventContext();
        }
        if (ctx.getDepth() >= MAX_DEPTH) {
            return List.of(BattleLog.of(BattleLog.TYPE_SKILL, caster.getName() + " 技能链过深，已截断"));
        }
        ctx.setActor(caster);
        ctx.setDepth(ctx.getDepth() + 1);

        GameFinishedSkill skill = finishedSkillService.getById(finishedSkillId);
        if (skill == null || !Integer.valueOf(1).equals(skill.getEnabled())) {
            return Collections.emptyList();
        }

        List<GameFinishedSkillEffect> effects = finishedSkillEffectService.listByFinishedSkillId(finishedSkillId);
        if (effects.isEmpty()) {
            return Collections.emptyList();
        }

        SkillTargetType targetType = SkillTargetType.parse(skill.getTargetType());
        if (targetType == null) {
            return Collections.emptyList();
        }

        List<BattleLog> logs = new ArrayList<>();

        triggerSlotEngineService.onFinishedSkillCast(state, caster, finishedSkillId, ctx.getDepth());

        List<TargetHit> hits = resolveTargetHits(state, caster, skill, targetType, ctx);
        boolean dealtDamage = false;
        for (TargetHit hit : hits) {
            for (GameFinishedSkillEffect effect : effects) {
                List<BattleLog> effectLogs = applyEffect(state, caster, hit.target(), effect, skill, ctx, hit.repeatIndex());
                logs.addAll(effectLogs);
                if (AdvancedEffectKind.STAT_FORMULA.name().equals(effect.getEffectKind())
                        || AdvancedEffectKind.FIXED_VALUE.name().equals(effect.getEffectKind())) {
                    if (EffectOutcomeType.DAMAGE.name().equals(effect.getOutcomeType())) {
                        dealtDamage = true;
                    }
                }
            }
        }

        if (dealtDamage) {
            TriggerEventContext attackCtx = copyCtx(ctx, caster, hits.isEmpty() ? null : hits.get(0).target());
            logs.addAll(triggerSlotEngineService.fireInstant(state, TriggerSlotType.ON_ATTACK, caster, attackCtx));
        }

        return logs;
    }

    private List<TargetHit> resolveTargetHits(BattleState state, BattleUnit caster, GameFinishedSkill skill,
                                                SkillTargetType targetType, TriggerEventContext ctx) {
        int param = skill.getTargetParam() != null ? skill.getTargetParam() : 1;
        return switch (targetType) {
            case SELF -> List.of(new TargetHit(caster, 0));
            case ALL_ALLIES -> listAllies(state, caster).stream().map(u -> new TargetHit(u, 0)).toList();
            case ALL_ENEMIES -> listEnemies(state, caster).stream().map(u -> new TargetHit(u, 0)).toList();
            case RANDOM_ONE_ENEMY -> pickRandomEnemies(state, caster, 1).stream().map(u -> new TargetHit(u, 0)).toList();
            case RANDOM_ENEMIES -> pickRandomEnemies(state, caster, param).stream().map(u -> new TargetHit(u, 0)).toList();
            case RANDOM_ONE_ENEMY_REPEAT -> {
                List<BattleUnit> one = pickRandomEnemies(state, caster, 1);
                if (one.isEmpty()) {
                    yield List.of();
                }
                List<TargetHit> repeated = new ArrayList<>();
                for (int i = 0; i < param; i++) {
                    repeated.add(new TargetHit(one.get(0), i));
                }
                yield repeated;
            }
            case CURRENT_ATTACK_TARGET -> {
                BattleUnit t = ctx.getPrimaryTarget();
                if (t != null && t.isAlive() && isEnemy(caster, t)) {
                    yield List.of(new TargetHit(t, 0));
                }
                yield pickRandomEnemies(state, caster, 1).stream().map(u -> new TargetHit(u, 0)).toList();
            }
            case FRONT_ROW_ENEMIES -> listEnemiesInRow(state, caster, true).stream().map(u -> new TargetHit(u, 0)).toList();
            case BACK_ROW_ENEMIES -> listEnemiesInRow(state, caster, false).stream().map(u -> new TargetHit(u, 0)).toList();
        };
    }

    private List<BattleLog> applyEffect(BattleState state, BattleUnit caster, BattleUnit target,
                                          GameFinishedSkillEffect effect, GameFinishedSkill skill,
                                          TriggerEventContext ctx, int repeatIndex) {
        AdvancedEffectKind kind = AdvancedEffectKind.parse(effect.getEffectKind());
        if (kind == null) {
            return Collections.emptyList();
        }
        return switch (kind) {
            case STAT_FORMULA, FIXED_VALUE -> applyOutcome(state, caster, target, effect, skill, ctx);
            case ACTION_VALUE -> applyActionDelta(target, effect);
        };
    }

    private List<BattleLog> applyOutcome(BattleState state, BattleUnit caster, BattleUnit target,
                                           GameFinishedSkillEffect effect, GameFinishedSkill skill,
                                           TriggerEventContext ctx) {
        if (target == null || !target.isAlive()) {
            return Collections.emptyList();
        }
        EffectOutcomeType outcome = EffectOutcomeType.parse(effect.getOutcomeType());
        if (outcome == null) {
            return Collections.emptyList();
        }

        BigDecimal amount = calcAmount(state, caster, effect);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        List<BattleLog> logs = new ArrayList<>();
        if (outcome == EffectOutcomeType.DAMAGE) {
            TriggerEventContext preCtx = copyCtx(ctx, caster, target);
            preCtx.setVictim(target);
            preCtx.setFinishedSkillId(skill.getId());
            preCtx.setFinishedSkillCasterSide(caster.getSide());
            logs.addAll(triggerSlotEngineService.fireInstant(state, TriggerSlotType.ON_TAKE_DAMAGE, target, preCtx));

            TriggerSlotType skillHitType = isEnemy(caster, target)
                    ? TriggerSlotType.ON_HIT_BY_ENEMY_FINISHED_SKILL
                    : TriggerSlotType.ON_HIT_BY_ALLY_FINISHED_SKILL;
            logs.addAll(triggerSlotEngineService.fireInstant(state, skillHitType, target, preCtx));

            BattleEngine.applyDamage(target, amount);
            logs.add(BattleLog.skillDamage(caster.getName(), target.getName(), skill.getName(),
                    amount.stripTrailingZeros().toPlainString(), !target.isAlive()));

            TriggerEventContext postCtx = copyCtx(preCtx, caster, target);
            postCtx.setDamageAmount(amount);
            logs.addAll(triggerSlotEngineService.fireAccumulated(state, caster, target, postCtx, amount, BigDecimal.ZERO));
            triggerSlotEngineService.recordHit(target);
            logs.addAll(triggerSlotEngineService.fireThreshold(state, TriggerSlotType.HIT_COUNT, target, postCtx));
        } else {
            int heal = amount.setScale(0, RoundingMode.CEILING).intValue();
            int nextHp = Math.min(target.getMaxHp(), target.getHp() + heal);
            int actualHeal = nextHp - target.getHp();
            target.setHp(nextHp);
            logs.add(BattleLog.skillHeal(caster.getName(), target.getName(), skill.getName(), String.valueOf(actualHeal)));

            TriggerEventContext healCtx = copyCtx(ctx, caster, target);
            healCtx.setHealAmount(BigDecimal.valueOf(actualHeal));
            logs.addAll(triggerSlotEngineService.fireInstant(state, TriggerSlotType.ON_HEAL, target, healCtx));
            logs.addAll(triggerSlotEngineService.fireAccumulated(state, caster, target, healCtx, BigDecimal.ZERO, BigDecimal.valueOf(actualHeal)));
        }
        BattleEngine.refreshAliveState(state);
        return logs;
    }

    private List<BattleLog> applyActionDelta(BattleUnit target, GameFinishedSkillEffect effect) {
        if (target == null || !target.isAlive() || effect.getActionDelta() == null) {
            return Collections.emptyList();
        }
        int max = target.getActionValue() != null ? target.getActionValue() : 0;
        int next = Math.max(0, Math.min(max, target.getActionBar() + effect.getActionDelta()));
        target.setActionBar(next);
        return List.of(BattleLog.of(BattleLog.TYPE_SKILL,
                target.getName() + " 行动值 " + (effect.getActionDelta() >= 0 ? "+" : "") + effect.getActionDelta()
                        + " → " + next));
    }

    private BigDecimal calcAmount(BattleState state, BattleUnit caster, GameFinishedSkillEffect effect) {
        AdvancedEffectKind kind = AdvancedEffectKind.parse(effect.getEffectKind());
        if (kind == AdvancedEffectKind.FIXED_VALUE) {
            return effect.getFixedValue() != null ? effect.getFixedValue() : BigDecimal.ZERO;
        }
        StatRefType statRef = StatRefType.parse(effect.getStatRef());
        BigDecimal base = BigDecimal.valueOf(readStat(caster, statRef));
        BigDecimal y = effect.getRatioY() != null ? effect.getRatioY() : BigDecimal.ONE;
        BigDecimal result = base.multiply(y);
        if (Integer.valueOf(1).equals(effect.getUseWeaponRatio())) {
            result = result.multiply(resolveWeaponDamageRatio(state, caster));
        }
        return result.setScale(1, RoundingMode.CEILING);
    }

    /** 读取施法者装备武器的伤害比例 damage_ratio */
    private BigDecimal resolveWeaponDamageRatio(BattleState state, BattleUnit caster) {
        if (caster.getWeaponDamageRatio() != null) {
            return caster.getWeaponDamageRatio();
        }
        if (state == null || !BattleUnit.SIDE_HERO.equals(caster.getSide())) {
            return BigDecimal.ONE;
        }
        List<String> itemIds = state.getHeroEquippedItemIds();
        if (itemIds == null || itemIds.isEmpty()) {
            return BigDecimal.ONE;
        }
        for (String itemId : itemIds) {
            GameWeapon weapon = gameWeaponService.getByItemId(itemId);
            if (weapon != null && weapon.getDamageRatio() != null) {
                return weapon.getDamageRatio();
            }
        }
        return BigDecimal.ONE;
    }

    private int readStat(BattleUnit unit, StatRefType statRef) {
        if (statRef == null) {
            return unit.getAttack() != null ? unit.getAttack() : 0;
        }
        return switch (statRef) {
            case ATTACK -> unit.getAttack() != null ? unit.getAttack() : 0;
            case DEFENSE -> unit.getDefense() != null ? unit.getDefense() : 0;
            case MAX_HP -> unit.getMaxHp() != null ? unit.getMaxHp() : 0;
        };
    }

    private List<BattleUnit> listAllies(BattleState state, BattleUnit caster) {
        return state.getUnits().stream()
                .filter(BattleUnit::isAlive)
                .filter(u -> Objects.equals(u.getSide(), caster.getSide()))
                .collect(Collectors.toList());
    }

    private List<BattleUnit> listEnemies(BattleState state, BattleUnit caster) {
        return state.getUnits().stream()
                .filter(BattleUnit::isAlive)
                .filter(u -> !Objects.equals(u.getSide(), caster.getSide()))
                .collect(Collectors.toList());
    }

    /**
     * 与战斗 UI 站位一致：1~3 怪单行（前/后排均指全部）；4 怪 2+2；5+ 怪前排 2、后排其余。
     */
    private List<BattleUnit> listEnemiesInRow(BattleState state, BattleUnit caster, boolean frontRow) {
        List<BattleUnit> enemies = listEnemies(state, caster);
        int count = enemies.size();
        if (count <= 3) {
            return enemies;
        }
        if (count == 4) {
            return frontRow ? enemies.subList(0, 2) : enemies.subList(2, 4);
        }
        return frontRow ? enemies.subList(0, 2) : enemies.subList(2, count);
    }

    private List<BattleUnit> pickRandomEnemies(BattleState state, BattleUnit caster, int count) {
        List<BattleUnit> enemies = listEnemies(state, caster);
        if (enemies.isEmpty() || count <= 0) {
            return List.of();
        }
        List<BattleUnit> pool = new ArrayList<>(enemies);
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return pool.subList(0, Math.min(count, pool.size()));
    }

    private boolean isEnemy(BattleUnit a, BattleUnit b) {
        return a != null && b != null && !Objects.equals(a.getSide(), b.getSide());
    }

    private TriggerEventContext copyCtx(TriggerEventContext src, BattleUnit actor, BattleUnit target) {
        TriggerEventContext c = new TriggerEventContext();
        c.setActor(actor);
        c.setPrimaryTarget(target);
        c.setVictim(src != null ? src.getVictim() : null);
        c.setFinishedSkillId(src != null ? src.getFinishedSkillId() : null);
        c.setFinishedSkillCasterSide(src != null ? src.getFinishedSkillCasterSide() : null);
        c.setDepth(src != null ? src.getDepth() : 0);
        return c;
    }

    private record TargetHit(BattleUnit target, int repeatIndex) {
    }
}
