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
    @Resource
    private SkillJsonHelper skillJsonHelper;
    @Resource
    private SkillExpressionService skillExpressionService;

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

        List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo> formulas =
                skillJsonHelper.readFormulas(skill.getFormulasJson());
        migrateLegacyFormulaMeta(formulas, skill);
        List<GameFinishedSkillEffect> effects = formulas.isEmpty()
                ? finishedSkillEffectService.listByFinishedSkillId(finishedSkillId)
                : List.of();
        if (formulas.isEmpty() && effects.isEmpty()) {
            return Collections.emptyList();
        }

        List<BattleLog> logs = new ArrayList<>();
        boolean dealtDamage = false;
        BattleUnit firstDamageTarget = null;

        if (!formulas.isEmpty()) {
            for (int fi = 0; fi < formulas.size(); fi++) {
                org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo formula = formulas.get(fi);
                if (!tryConsumeFormulaCast(state, caster, finishedSkillId, fi, formula.getMaxCastCount())) {
                    continue;
                }
                SkillTargetType targetType = SkillTargetType.parse(formula.getTargetType());
                if (targetType == null) {
                    targetType = SkillTargetType.parse(skill.getTargetType());
                }
                if (targetType == null) {
                    continue;
                }
                List<TargetHit> hits = resolveTargetHits(state, caster, targetType, formula.getTargetParam(),
                        formula.getHitFrequency(), ctx);
                for (TargetHit hit : hits) {
                    List<BattleLog> effectLogs = applyFormula(state, caster, hit.target(), formula, skill, ctx, hit.repeatIndex());
                    logs.addAll(effectLogs);
                    if (SkillFormulaOutcome.DAMAGE.name().equals(formula.getOutcome())) {
                        dealtDamage = true;
                        if (firstDamageTarget == null) {
                            firstDamageTarget = hit.target();
                        }
                    }
                }
            }
        } else {
            SkillTargetType targetType = SkillTargetType.parse(skill.getTargetType());
            if (targetType == null) {
                return Collections.emptyList();
            }
            List<TargetHit> hits = resolveTargetHits(state, caster, targetType, skill.getTargetParam(),
                    skill.getHitFrequency(), ctx);
            for (TargetHit hit : hits) {
                for (GameFinishedSkillEffect effect : effects) {
                    List<BattleLog> effectLogs = applyEffect(state, caster, hit.target(), effect, skill, ctx, hit.repeatIndex());
                    logs.addAll(effectLogs);
                    if (AdvancedEffectKind.STAT_FORMULA.name().equals(effect.getEffectKind())
                            || AdvancedEffectKind.FIXED_VALUE.name().equals(effect.getEffectKind())) {
                        if (EffectOutcomeType.DAMAGE.name().equals(effect.getOutcomeType())) {
                            dealtDamage = true;
                            if (firstDamageTarget == null) {
                                firstDamageTarget = hit.target();
                            }
                        }
                    }
                }
            }
        }

        if (dealtDamage) {
            TriggerEventContext attackCtx = copyCtx(ctx, caster, firstDamageTarget);
            logs.addAll(triggerSlotEngineService.fireInstant(state, TriggerSlotType.ON_ATTACK, caster, attackCtx));
        }

        ctx.setFinishedSkillId(finishedSkillId);
        logs.addAll(triggerSlotEngineService.onFinishedSkillCast(state, caster, finishedSkillId, ctx.getDepth()));

        return logs;
    }

    /** 旧数据：公式未带目标/频率时，回退到技能级字段 */
    private void migrateLegacyFormulaMeta(List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo> formulas,
                                          GameFinishedSkill skill) {
        if (formulas == null || formulas.isEmpty() || skill == null) {
            return;
        }
        boolean legacy = formulas.stream()
                .allMatch(f -> f.getTargetType() == null || f.getTargetType().isBlank());
        if (!legacy) {
            return;
        }
        for (org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo fg : formulas) {
            fg.setTargetType(skill.getTargetType());
            fg.setTargetParam(skill.getTargetParam());
            fg.setHitFrequency(skill.getHitFrequency() != null ? skill.getHitFrequency() : 1);
            fg.setMaxCastCount(skill.getMaxCastCount());
        }
    }

    private boolean tryConsumeFormulaCast(BattleState state, BattleUnit caster, String finishedSkillId,
                                          int formulaIndex, Integer maxCastCount) {
        if (maxCastCount == null || maxCastCount <= 0) {
            return true;
        }
        if (state.getTriggerCounters() == null) {
            state.setTriggerCounters(new BattleTriggerCounters());
        }
        BattleTriggerCounters counters = state.getTriggerCounters();
        if (counters.getFormulaCastCount() == null) {
            counters.setFormulaCastCount(new HashMap<>());
        }
        String key = finishedSkillId + "#" + formulaIndex;
        Map<String, Integer> unitMap = counters.getFormulaCastCount()
                .computeIfAbsent(caster.getUnitId(), k -> new HashMap<>());
        int used = unitMap.getOrDefault(key, 0);
        if (used >= maxCastCount) {
            return false;
        }
        unitMap.put(key, used + 1);
        return true;
    }

    private List<TargetHit> resolveTargetHits(BattleState state, BattleUnit caster,
                                                SkillTargetType targetType, Integer targetParam,
                                                Integer hitFrequency, TriggerEventContext ctx) {
        int frequency = hitFrequency != null && hitFrequency > 0 ? hitFrequency : 1;
        List<BattleUnit> baseTargets = switch (targetType) {
            case SELF -> List.of(caster);
            case ALL_ALLIES -> listAllies(state, caster);
            case ALL_ENEMIES -> listEnemies(state, caster);
            case FRONT_ROW_ALL -> listEnemiesInRow(state, caster, true);
            case MID_ROW_ALL -> listEnemiesInMidRow(state, caster);
            case BACK_ROW_ALL -> listEnemiesInRow(state, caster, false);
            case RANDOM_1 -> pickRandomAny(state, caster, 1);
            case FIRST_TARGET -> {
                BattleUnit first = firstTargetByGrid(state, caster);
                yield first != null ? List.of(first) : List.of();
            }
            case MAIN_TARGET, CURRENT_ATTACK_TARGET -> {
                BattleUnit t = ctx.getPrimaryTarget();
                if (t != null && t.isAlive() && isEnemy(caster, t)) {
                    yield List.of(t);
                }
                yield pickRandomEnemies(state, caster, 1);
            }
            case RANDOM_ONE_ENEMY -> pickRandomEnemies(state, caster, 1);
            case RANDOM_ENEMIES -> pickRandomEnemies(state, caster, targetParam != null ? targetParam : 1);
            case FRONT_ROW_RANDOM_ONE_ENEMY -> pickRandomEnemiesFromRow(state, caster, true, 1);
            case BACK_ROW_RANDOM_ONE_ENEMY -> pickRandomEnemiesFromRow(state, caster, false, 1);
            case RANDOM_ONE_ENEMY_REPEAT -> pickRandomEnemies(state, caster, 1);
            case FRONT_ROW_ENEMIES -> listEnemiesInRow(state, caster, true);
            case BACK_ROW_ENEMIES -> listEnemiesInRow(state, caster, false);
        };
        if (targetType == SkillTargetType.RANDOM_ONE_ENEMY_REPEAT) {
            frequency = targetParam != null && targetParam > 0 ? targetParam : frequency;
        }
        List<TargetHit> hits = new ArrayList<>();
        for (BattleUnit target : baseTargets) {
            for (int i = 0; i < frequency; i++) {
                hits.add(new TargetHit(target, i));
            }
        }
        return hits;
    }

    private List<BattleUnit> listEnemiesInMidRow(BattleState state, BattleUnit caster) {
        return listEnemies(state, caster).stream()
                .filter(u -> u.getSlotRow() != null && u.getSlotRow() == 1)
                .collect(Collectors.toList());
    }

    private BattleUnit firstTargetByGrid(BattleState state, BattleUnit caster) {
        return listEnemies(state, caster).stream()
                .sorted(Comparator
                        .comparing((BattleUnit u) -> u.getSlotRow() == null ? 99 : u.getSlotRow())
                        .thenComparing(u -> u.getSlotCol() == null ? 99 : u.getSlotCol()))
                .findFirst()
                .orElse(null);
    }

    private List<BattleUnit> pickRandomAny(BattleState state, BattleUnit caster, int count) {
        return pickRandomEnemies(state, caster, count);
    }

    private List<BattleLog> applyFormula(BattleState state, BattleUnit caster, BattleUnit target,
                                         org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo formula,
                                         GameFinishedSkill skill, TriggerEventContext ctx, int repeatIndex) {
        if (target == null || !target.isAlive() || formula == null) {
            return Collections.emptyList();
        }
        var reader = skillExpressionService.unitReader(caster, Map.of());
        BigDecimal amount = skillExpressionService.evalFormula(formula, reader);
        amount = amount.setScale(1, RoundingMode.CEILING);
        SkillFormulaOutcome outcome = SkillFormulaOutcome.parse(formula.getOutcome());
        if (outcome == null) {
            return Collections.emptyList();
        }
        List<BattleLog> logs = new ArrayList<>();
        switch (outcome) {
            case DAMAGE -> {
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
                logs.add(BattleLog.skillDamage(caster.getName(), target.getName(),
                        BattleLog.buildSkillDisplayLabel(skill),
                        amount.stripTrailingZeros().toPlainString(),
                        "公式", !target.isAlive()));
                TriggerEventContext postCtx = copyCtx(preCtx, caster, target);
                postCtx.setDamageAmount(amount);
                logs.addAll(triggerSlotEngineService.fireAccumulated(state, caster, target, postCtx, amount, BigDecimal.ZERO));
                logs.addAll(triggerSlotEngineService.recordHit(state, target, skill.getId(), postCtx));
                logs.addAll(triggerSlotEngineService.fireThreshold(state, TriggerSlotType.HIT_COUNT, target, postCtx));
            }
            case HEAL -> {
                int heal = amount.setScale(0, RoundingMode.CEILING).intValue();
                int nextHp = Math.min(target.getMaxHp(), target.getHp() + heal);
                int actualHeal = nextHp - target.getHp();
                target.setHp(nextHp);
                logs.add(BattleLog.skillHeal(caster.getName(), target.getName(),
                        BattleLog.buildSkillDisplayLabel(skill), String.valueOf(actualHeal)));
                TriggerEventContext healCtx = copyCtx(ctx, caster, target);
                healCtx.setHealAmount(BigDecimal.valueOf(actualHeal));
                logs.addAll(triggerSlotEngineService.fireInstant(state, TriggerSlotType.ON_HEAL, target, healCtx));
                logs.addAll(triggerSlotEngineService.fireAccumulated(state, caster, target, healCtx,
                        BigDecimal.ZERO, BigDecimal.valueOf(actualHeal)));
            }
            case ACTION_INC -> {
                int bar = target.getActionBar() != null ? target.getActionBar() : 0;
                int max = target.getActionValue() != null ? target.getActionValue() : 0;
                int delta = Math.max(0, amount.intValue());
                target.setActionBar(Math.min(max, bar + delta));
                logs.add(BattleLog.of(BattleLog.TYPE_SKILL, target.getName() + " 行动值 +" + delta));
            }
            case ACTION_DEC -> {
                int bar = target.getActionBar() != null ? target.getActionBar() : 0;
                int delta = Math.max(0, amount.intValue());
                target.setActionBar(Math.max(0, bar - delta));
                logs.add(BattleLog.of(BattleLog.TYPE_SKILL, target.getName() + " 行动值 -" + delta));
            }
            case ENERGY_MAX_INC -> {
                int max = target.getActionValue() != null ? target.getActionValue() : 0;
                int delta = Math.max(0, amount.intValue());
                target.setActionValue(Math.max(10, max + delta));
                logs.add(BattleLog.of(BattleLog.TYPE_SKILL, target.getName() + " 最大能量 +" + delta));
            }
            case ENERGY_MAX_DEC -> {
                int max = target.getActionValue() != null ? target.getActionValue() : 0;
                int delta = Math.max(0, amount.intValue());
                target.setActionValue(Math.max(10, max - delta));
                logs.add(BattleLog.of(BattleLog.TYPE_SKILL, target.getName() + " 最大能量 -" + delta));
            }
        }
        BattleEngine.refreshAliveState(state);
        return logs;
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
            String formula = buildStatFormulaText(state, caster, effect);
            String skillLabel = BattleLog.buildSkillDisplayLabel(skill);
            logs.add(BattleLog.skillDamage(caster.getName(), target.getName(), skillLabel,
                    amount.stripTrailingZeros().toPlainString(), formula, !target.isAlive()));

            TriggerEventContext postCtx = copyCtx(preCtx, caster, target);
            postCtx.setDamageAmount(amount);
            logs.addAll(triggerSlotEngineService.fireAccumulated(state, caster, target, postCtx, amount, BigDecimal.ZERO));
            logs.addAll(triggerSlotEngineService.recordHit(state, target, skill.getId(), postCtx));
            logs.addAll(triggerSlotEngineService.fireThreshold(state, TriggerSlotType.HIT_COUNT, target, postCtx));
        } else {
            int heal = amount.setScale(0, RoundingMode.CEILING).intValue();
            int nextHp = Math.min(target.getMaxHp(), target.getHp() + heal);
            int actualHeal = nextHp - target.getHp();
            target.setHp(nextHp);
            logs.add(BattleLog.skillHeal(caster.getName(), target.getName(),
                    BattleLog.buildSkillDisplayLabel(skill), String.valueOf(actualHeal)));

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
            case CUR_HP -> unit.getHp() != null ? unit.getHp() : 0;
            case MAX_ACTION -> unit.getActionValue() != null ? unit.getActionValue() : 0;
            case CUR_ACTION -> unit.getActionBar() != null ? unit.getActionBar() : 0;
            case WEAPON_ATTACK -> unit.getWeaponAttack() != null ? unit.getWeaponAttack() : 0;
            case WEAPON_DAMAGE_RATIO -> unit.getWeaponDamageRatio() != null
                    ? unit.getWeaponDamageRatio().intValue() : 1;
        };
    }

    private String buildStatFormulaText(BattleState state, BattleUnit caster, GameFinishedSkillEffect effect) {
        AdvancedEffectKind kind = AdvancedEffectKind.parse(effect.getEffectKind());
        if (kind == AdvancedEffectKind.FIXED_VALUE) {
            BigDecimal val = effect.getFixedValue() != null ? effect.getFixedValue() : BigDecimal.ZERO;
            return "(固定值 " + val.stripTrailingZeros().toPlainString() + ")";
        }
        if (kind != AdvancedEffectKind.STAT_FORMULA) {
            return null;
        }
        StatRefType statRef = StatRefType.parse(effect.getStatRef());
        String statLabel = statRef != null ? statRef.getLabel()
                : org.wx.core.wxBusiness.game.entity.enums.SkillReadResolver.resolveLabel(effect.getStatRef());
        if (statLabel == null || statLabel.isBlank()) {
            statLabel = "攻击";
        }
        int base = readStat(caster, statRef);
        BigDecimal y = effect.getRatioY() != null ? effect.getRatioY() : BigDecimal.ONE;
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(statLabel).append(" ").append(base)
                .append(" × ").append(formatRatioPercent(y));
        if (Integer.valueOf(1).equals(effect.getUseWeaponRatio())) {
            BigDecimal weaponRatio = resolveWeaponDamageRatio(state, caster);
            sb.append(" × 武器 ").append(formatRatioPercent(weaponRatio));
        }
        sb.append(")");
        return sb.toString();
    }

    private String formatRatioPercent(BigDecimal ratio) {
        if (ratio == null) {
            return "100%";
        }
        return ratio.multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros()
                .toPlainString() + "%";
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
     * 前排：从 1 排往 3 排扫，第一排有存活敌人为前排；
     * 后排：从 3 排往 1 排扫，第一排有存活敌人为后排。
     * 无站位数据时回退为全体敌人。
     */
    private List<BattleUnit> listEnemiesInRow(BattleState state, BattleUnit caster, boolean frontRow) {
        List<BattleUnit> enemies = listEnemies(state, caster);
        if (enemies.isEmpty()) {
            return enemies;
        }
        boolean anySlot = enemies.stream().anyMatch(u -> u.getSlotRow() != null);
        if (!anySlot) {
            return enemies;
        }
        return frontRow
                ? BattleFormation.unitsOnFrontRow(enemies)
                : BattleFormation.unitsOnBackRow(enemies);
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

    private List<BattleUnit> pickRandomEnemiesFromRow(BattleState state, BattleUnit caster, boolean frontRow, int count) {
        // 前/后排扫描本身已含「该方向无人则继续往里找」，不再弹性改打另一排
        List<BattleUnit> row = listEnemiesInRow(state, caster, frontRow);
        if (row.isEmpty() || count <= 0) {
            return List.of();
        }
        List<BattleUnit> pool = new ArrayList<>(row);
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
        c.setDamageAmount(src != null ? src.getDamageAmount() : BigDecimal.ZERO);
        c.setHealAmount(src != null ? src.getHealAmount() : BigDecimal.ZERO);
        c.setDepth(src != null ? src.getDepth() : 0);
        return c;
    }

    private record TargetHit(BattleUnit target, int repeatIndex) {
    }
}
