package org.wx.core.wxBusiness.game.engine;

import org.wx.core.wxBusiness.game.entity.BattleFormation;
import org.wx.core.wxBusiness.game.entity.BattleLog;
import org.wx.core.wxBusiness.game.entity.BattleState;
import org.wx.core.wxBusiness.game.entity.BattleUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
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
        tickActionBars(state, null);
    }

    public static void tickActionBars(BattleState state, BiConsumer<BattleUnit, Integer> onUnitTick) {
        if (!state.isRunning()) {
            return;
        }
        int gain = state.getActionTickGain() != null ? state.getActionTickGain() : DEFAULT_TICK_GAIN;
        state.advanceTimeline();
        for (BattleUnit unit : state.getUnits()) {
            if (!unit.isAlive()) {
                continue;
            }
            int next = ceilActionBar(unit.getActionBar() + gain);
            unit.setActionBar(Math.min(next, unit.getActionValue()));
            if (onUnitTick != null) {
                onUnitTick.accept(unit, gain);
            }
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
        return BigDecimal.valueOf(raw).setScale(1, RoundingMode.CEILING);
    }

    public static int damageToInt(BigDecimal damage) {
        return damage.setScale(0, RoundingMode.CEILING).intValue();
    }

    /** 默认选敌：优先当前前排随机 1 个；前排无人则全体随机 */
    public static BattleUnit pickTarget(BattleState state, BattleUnit actor) {
        if (BattleUnit.SIDE_HERO.equals(actor.getSide())) {
            List<BattleUnit> monsters = state.getUnits().stream()
                    .filter(u -> BattleUnit.SIDE_MONSTER.equals(u.getSide()))
                    .filter(BattleUnit::isAlive)
                    .collect(Collectors.toList());
            if (monsters.isEmpty()) {
                return null;
            }
            List<BattleUnit> front = BattleFormation.unitsOnFrontRow(monsters);
            List<BattleUnit> pool = front.isEmpty() ? monsters : front;
            return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        }
        return state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_HERO.equals(u.getSide()))
                .filter(BattleUnit::isAlive)
                .findFirst()
                .orElse(null);
    }

    public static void applyDamage(BattleUnit target, BigDecimal damage) {
        int hurt = damageToInt(damage);
        int nextHp = Math.max(0, target.getHp() - hurt);
        target.setHp(nextHp);
        if (nextHp <= 0) {
            target.setAlive(false);
            target.setActionBar(0);
        }
    }

    public static void resetActionBar(BattleUnit unit) {
        unit.setActionBar(0);
    }

    public static void advanceUntilReady(BattleState state) {
        advanceUntilReady(state, null);
    }

    public static void advanceUntilReady(BattleState state, BiConsumer<BattleUnit, Integer> onUnitTick) {
        int guard = 0;
        while (state.isRunning() && pickReadyUnit(state) == null && guard < MAX_INTERNAL_TICKS) {
            tickActionBars(state, onUnitTick);
            guard++;
        }
    }

    public static BattleLog performAction(BattleState state, BattleUnit actor) {
        BattleUnit target = pickTarget(state, actor);
        if (target == null) {
            return BattleLog.of(BattleLog.TYPE_ACTION, actor.getName() + " 无有效目标");
        }
        BigDecimal damage = calcDamage(actor.getAttack());
        applyDamage(target, damage);

        String damageText = damage.stripTrailingZeros().toPlainString();
        return BattleLog.action(actor.getName(), target.getName(), damageText, !target.isAlive());
    }

    public static void refreshAliveState(BattleState state) {
        for (BattleUnit unit : state.getUnits()) {
            unit.setAlive(unit.getHp() != null && unit.getHp() > 0);
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
