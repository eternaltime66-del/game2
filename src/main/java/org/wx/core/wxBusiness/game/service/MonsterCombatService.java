package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.BattleUnit;
import org.wx.core.wxBusiness.game.entity.GameMonsterPassive;
import org.wx.core.wxBusiness.game.entity.GamePassiveSkill;
import org.wx.core.wxBusiness.game.entity.enums.PassiveConditionType;
import org.wx.core.wxBusiness.game.entity.enums.PassiveEffectType;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MonsterCombatService {

    @Resource
    private GameMonsterPassiveService monsterPassiveService;
    @Resource
    private GamePassiveSkillService passiveSkillService;

    public void applyPassives(BattleUnit unit, String monsterId) {
        if (unit == null || monsterId == null || monsterId.isBlank()) {
            return;
        }
        int flatAttack = 0;
        int flatDefense = 0;
        int flatHp = 0;
        BigDecimal attackMul = BigDecimal.ONE;
        BigDecimal defenseMul = BigDecimal.ONE;
        BigDecimal hpMul = BigDecimal.ONE;
        BigDecimal actionFactor = BigDecimal.ONE;

        for (GameMonsterPassive binding : monsterPassiveService.listEnabledByMonsterId(monsterId)) {
            GamePassiveSkill passive = passiveSkillService.getById(binding.getPassiveSkillId());
            if (passive == null || !Integer.valueOf(1).equals(passive.getEnabled())) {
                continue;
            }
            PassiveConditionType conditionType = PassiveConditionType.parse(passive.getConditionType());
            if (conditionType != null && conditionType != PassiveConditionType.NONE) {
                continue;
            }
            PassiveEffectType effectType = PassiveEffectType.parse(passive.getEffectType());
            if (effectType == null || passive.getEffectValue() == null) {
                continue;
            }
            BigDecimal val = passive.getEffectValue();
            switch (effectType) {
                case ATTACK_FLAT -> flatAttack += val.setScale(0, RoundingMode.CEILING).intValue();
                case DEFENSE_FLAT -> flatDefense += val.setScale(0, RoundingMode.CEILING).intValue();
                case HP_FLAT -> flatHp += val.setScale(0, RoundingMode.CEILING).intValue();
                case ATTACK_PCT -> attackMul = attackMul.multiply(pctMultiplier(val));
                case DEFENSE_PCT -> defenseMul = defenseMul.multiply(pctMultiplier(val));
                case HP_PCT -> hpMul = hpMul.multiply(pctMultiplier(val));
                case ACTION_VALUE_REDUCE_PCT -> actionFactor = actionFactor.multiply(reducePctMultiplier(val));
                default -> {
                }
            }
        }

        if (flatAttack == 0 && flatDefense == 0 && flatHp == 0
                && attackMul.compareTo(BigDecimal.ONE) == 0
                && defenseMul.compareTo(BigDecimal.ONE) == 0
                && hpMul.compareTo(BigDecimal.ONE) == 0
                && actionFactor.compareTo(BigDecimal.ONE) == 0) {
            return;
        }

        int attackBase = unit.getAttack() != null ? unit.getAttack() : 0;
        int defenseBase = unit.getDefense() != null ? unit.getDefense() : 0;
        int hpBase = unit.getMaxHp() != null ? unit.getMaxHp() : 0;

        unit.setAttack(applyFlatThenPct(attackBase + flatAttack, attackMul));
        unit.setDefense(applyFlatThenPct(defenseBase + flatDefense, defenseMul));
        int totalMaxHp = applyFlatThenPct(hpBase + flatHp, hpMul);
        unit.setMaxHp(totalMaxHp);
        if (unit.getHp() != null) {
            unit.setHp(Math.min(unit.getHp(), totalMaxHp));
        }
        if (unit.getActionValue() != null && actionFactor.compareTo(BigDecimal.ONE) != 0) {
            unit.setActionValue(Math.max(1, BigDecimal.valueOf(unit.getActionValue())
                    .multiply(actionFactor)
                    .setScale(0, RoundingMode.CEILING)
                    .intValue()));
        }
    }

    private int applyFlatThenPct(int base, BigDecimal mul) {
        return BigDecimal.valueOf(base).multiply(mul).setScale(0, RoundingMode.CEILING).intValue();
    }

    private BigDecimal pctMultiplier(BigDecimal pct) {
        return BigDecimal.ONE.add(pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }

    private BigDecimal reducePctMultiplier(BigDecimal pct) {
        return BigDecimal.ONE.subtract(pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }
}
