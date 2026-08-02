package org.wx.core.wxBusiness.game.engine;

import org.wx.core.wxBusiness.game.entity.BattleLog;
import org.wx.core.wxBusiness.game.entity.BattleState;
import org.wx.core.wxBusiness.game.entity.BattleUnit;
import org.wx.core.wxBusiness.game.service.CombatDamageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * PVE 基础战斗规则
 */
public class BattleEngine {

    public static final int DEFAULT_TICK_GAIN = 1;
    private static final int MAX_INTERNAL_TICKS = 10000;

    private BattleEngine() {
    }

    public static void tickActionBars(BattleState state) {
        if (!state.isRunning()) {
            return;
        }
        for (BattleUnit unit : state.getUnits()) {
            if (!unit.isAlive()) {
                continue;
            }
            int next = ceilActionBar(unit.getActionBar() + state.getActionTickGain());
            unit.setActionBar(Math.min(next, unit.getActionValue()));
        }
    }

    public static int ceilActionBar(double value) {
        return (int) Math.ceil(value);
    }

    public static List<BattleUnit> listReadyUnits(BattleState state) {
        return state.getUnits().stream()
                .filter(BattleUnit::isAlive)
                .filter(u -> u.getActionBar() >= u.getActionValue())
                .sorted(Comparator
                        .comparing((BattleUnit u) -> u.getActionBar() - u.getActionValue()).reversed()
                        .thenComparing(u -> BattleUnit.SIDE_HERO.equals(u.getSide()) ? 0 : 1)
                        .thenComparing(BattleUnit::getUnitId))
                .collect(Collectors.toList());
    }

    public static BattleUnit pickReadyUnit(BattleState state) {
        List<BattleUnit> ready = listReadyUnits(state);
        return ready.isEmpty() ? null : ready.get(0);
    }

    public static BigDecimal calcDamage(int attack) {
        double rate = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
        double raw = attack * rate;
        return BigDecimal.valueOf(raw).setScale(CombatDamageService.DAMAGE_SCALE, RoundingMode.HALF_UP);
    }

    public static void applyDamage(BattleUnit target, BigDecimal hurt) {
        if (hurt == null) {
            hurt = BigDecimal.ZERO;
        }
        BigDecimal current = target.getHp() != null ? target.getHp() : BigDecimal.ZERO;
        BigDecimal nextHp = current.subtract(hurt)
                .max(BigDecimal.ZERO)
                .setScale(CombatDamageService.DAMAGE_SCALE, RoundingMode.HALF_UP);
        target.setHp(nextHp);
        if (nextHp.compareTo(BigDecimal.ZERO) <= 0) {
            target.setAlive(false);
            target.setActionBar(0);
        }
    }

    public static BattleUnit pickTarget(BattleState state, BattleUnit actor) {
        if (BattleUnit.SIDE_HERO.equals(actor.getSide())) {
            List<BattleUnit> monsters = state.getUnits().stream()
                    .filter(u -> BattleUnit.SIDE_MONSTER.equals(u.getSide()))
                    .filter(BattleUnit::isAlive)
                    .collect(Collectors.toList());
            if (monsters.isEmpty()) {
                return null;
            }
            return monsters.get(ThreadLocalRandom.current().nextInt(monsters.size()));
        }
        return state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_HERO.equals(u.getSide()))
                .filter(BattleUnit::isAlive)
                .findFirst()
                .orElse(null);
    }

    public static BattleLog performAction(BattleState state, BattleUnit actor) {
        BattleUnit target = pickTarget(state, actor);
        if (target == null) {
            return BattleLog.of(BattleLog.TYPE_ACTION, actor.getName() + " 无有效目标");
        }
        BigDecimal outputDamage = calcDamage(actor.getAttack());
        int defense = target.getDefense() != null ? target.getDefense() : 0;
        BigDecimal hurt = CombatDamageService.calcReceivedDamage(outputDamage, defense);
        applyDamage(target, hurt);
        resetActionBar(actor);

        String damageText = CombatDamageService.formatDamage(hurt);
        return BattleLog.action(actor.getName(), target.getName(), damageText, !target.isAlive());
    }

    public static void resetActionBar(BattleUnit unit) {
        unit.setActionBar(0);
    }

    public static void advanceUntilReady(BattleState state) {
        int guard = 0;
        while (state.isRunning() && pickReadyUnit(state) == null && guard < MAX_INTERNAL_TICKS) {
            tickActionBars(state);
            guard++;
        }
    }

    public static void refreshAliveState(BattleState state) {
        for (BattleUnit unit : state.getUnits()) {
            unit.setAlive(unit.getHp() != null && unit.getHp().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    public static boolean allMonstersDead(BattleState state) {
        return state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_MONSTER.equals(u.getSide()))
                .noneMatch(BattleUnit::isAlive);
    }

    public static boolean heroDead(BattleState state) {
        return state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_HERO.equals(u.getSide()))
                .noneMatch(BattleUnit::isAlive);
    }

    public static BattleUnit findHero(BattleState state) {
        return state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_HERO.equals(u.getSide()))
                .findFirst()
                .orElse(null);
    }

    public static List<BattleUnit> listAliveMonsters(BattleState state) {
        return state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_MONSTER.equals(u.getSide()))
                .filter(BattleUnit::isAlive)
                .collect(Collectors.toList());
    }

    public static void removeDeadMonsters(BattleState state) {
        state.setUnits(state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_HERO.equals(u.getSide())
                        || (BattleUnit.SIDE_MONSTER.equals(u.getSide()) && u.isAlive()))
                .collect(Collectors.toCollection(ArrayList::new)));
    }
}
