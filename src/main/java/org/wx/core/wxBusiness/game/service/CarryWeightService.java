package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.BattleBagItemVo;
import org.wx.core.wxBusiness.game.entity.BattleBagVo;
import org.wx.core.wxBusiness.game.entity.GameBattleBag;
import org.wx.core.wxBusiness.game.entity.GameHero;
import org.wx.core.wxBusiness.game.entity.GameItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CarryWeightService {

    public static final BigDecimal DEFAULT_OPTIMAL = BigDecimal.valueOf(10);

    public BattleBagVo buildWeightSummary(GameHero hero, List<GameBattleBag> bagRows, List<GameItem> items,
                                          HeroCombatService.HeroCombatContext combat) {
        BigDecimal bagWeight = calcBagWeight(bagRows, items, combat);
        BigDecimal equipWeight = calcEquipWeight(combat);
        BigDecimal totalWeight = bagWeight.add(equipWeight);
        BigDecimal optimal = hero != null && hero.getOptimalCarryWeight() != null
                ? hero.getOptimalCarryWeight() : DEFAULT_OPTIMAL;
        BigDecimal excessPercent = calcExcessPercent(totalWeight, optimal);

        int unarmedAction = combat != null ? combat.getUnarmedActionValue()
                : (hero != null && hero.getActionValue() != null ? hero.getActionValue() : GameHero.DEFAULT_ACTION_VALUE);
        int baseAction = combat != null ? combat.getBaseActionValue() : unarmedAction;
        int effectiveAction = calcEffectiveActionValue(baseAction, totalWeight, optimal);

        BattleBagVo vo = new BattleBagVo();
        vo.setBagWeight(bagWeight);
        vo.setEquipWeight(equipWeight);
        vo.setTotalWeight(totalWeight);
        vo.setOptimalCarryWeight(optimal);
        vo.setExcessPercent(excessPercent);
        vo.setUnarmedActionValue(unarmedAction);
        vo.setBaseActionValue(baseAction);
        vo.setEffectiveActionValue(effectiveAction);
        if (combat != null) {
            vo.setHeroAttack(combat.getHeroAttack());
            vo.setEquipAttack(combat.getEquipAttack());
            vo.setTotalAttack(combat.getTotalAttack());
            vo.setNormalAttackDamage(combat.getNormalAttackDamage());
            vo.setHeroDefense(combat.getHeroDefense());
            vo.setEquipDefense(combat.getEquipDefense());
            vo.setTotalDefense(combat.getTotalDefense());
            vo.setEquipBonusHp(combat.getEquipBonusHp());
            vo.setTotalMaxHp(combat.getTotalMaxHp());
            vo.setDamageRatio(combat.getDamageRatio());
            vo.setEquippedWeapon(combat.getEquippedWeaponVo());
        } else {
            int heroAttack = hero != null && hero.getAttack() != null ? hero.getAttack() : GameHero.DEFAULT_ATTACK;
            int heroDefense = hero != null && hero.getDefense() != null ? hero.getDefense() : GameHero.DEFAULT_DEFENSE;
            int heroMaxHp = hero != null && hero.getMaxHp() != null ? hero.getMaxHp() : GameHero.DEFAULT_MAX_HP;
            vo.setHeroAttack(heroAttack);
            vo.setEquipAttack(0);
            vo.setTotalAttack(heroAttack);
            vo.setNormalAttackDamage(heroAttack);
            vo.setHeroDefense(heroDefense);
            vo.setEquipDefense(0);
            vo.setTotalDefense(heroDefense);
            vo.setEquipBonusHp(0);
            vo.setTotalMaxHp(heroMaxHp);
            vo.setDamageRatio(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP));
        }
        return vo;
    }

    public BigDecimal calcBagWeight(List<GameBattleBag> bagRows, List<GameItem> items,
                                    HeroCombatService.HeroCombatContext combat) {
        if (bagRows == null || bagRows.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        java.util.Map<String, GameItem> itemMap = items.stream()
                .collect(java.util.stream.Collectors.toMap(GameItem::getId, i -> i, (a, b) -> a));
        BigDecimal total = BigDecimal.ZERO;
        for (GameBattleBag row : bagRows) {
            int qty = row.getQuantity() != null ? row.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            GameItem item = itemMap.get(row.getItemId());
            BigDecimal unit = item != null && item.getWeight() != null ? item.getWeight() : BigDecimal.valueOf(0.1);
            total = total.add(unit.multiply(BigDecimal.valueOf(qty)));
        }
        return total.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal calcEquipWeight(HeroCombatService.HeroCombatContext combat) {
        if (combat == null || combat.getEquippedItems() == null || combat.getEquippedItems().isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (GameItem item : combat.getEquippedItems().values()) {
            BigDecimal unit = item.getWeight() != null ? item.getWeight() : BigDecimal.valueOf(0.1);
            total = total.add(unit);
        }
        return total.setScale(1, RoundingMode.HALF_UP);
    }

    /** @deprecated use calcBagWeight with combat context */
    public BigDecimal calcBagWeight(List<GameBattleBag> bagRows, List<GameItem> items) {
        return calcBagWeight(bagRows, items, null);
    }

    public BattleBagVo buildWeightSummary(GameHero hero, List<GameBattleBag> bagRows, List<GameItem> items) {
        return buildWeightSummary(hero, bagRows, items, null);
    }

    public BigDecimal calcExcessPercent(BigDecimal totalWeight, BigDecimal optimal) {
        if (optimal == null || optimal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (totalWeight == null || totalWeight.compareTo(optimal) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalWeight.subtract(optimal)
                .multiply(BigDecimal.valueOf(100))
                .divide(optimal, 2, RoundingMode.HALF_UP);
    }

    /** 每超重 1%，行动值 ×1.02（叠乘） */
    public int calcEffectiveActionValue(int baseActionValue, BigDecimal totalWeight, BigDecimal optimal) {
        if (baseActionValue <= 0) {
            return baseActionValue;
        }
        BigDecimal excess = calcExcessPercent(totalWeight, optimal);
        if (excess.compareTo(BigDecimal.ZERO) <= 0) {
            return baseActionValue;
        }
        double multiplier = Math.pow(1.02d, excess.doubleValue());
        return (int) Math.ceil(baseActionValue * multiplier);
    }

    public void fillItemWeight(BattleBagItemVo vo, GameItem item, int quantity) {
        BigDecimal unit = item != null && item.getWeight() != null ? item.getWeight() : BigDecimal.valueOf(0.1);
        vo.setUnitWeight(unit);
        vo.setTotalWeight(unit.multiply(BigDecimal.valueOf(quantity)).setScale(1, RoundingMode.HALF_UP));
    }
}
