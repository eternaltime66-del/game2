package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.GameHeroEquip;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameItemPassive;
import org.wx.core.wxBusiness.game.entity.GamePassiveSkill;
import org.wx.core.wxBusiness.game.entity.HeroPassiveDetailVo;
import org.wx.core.wxBusiness.game.entity.enums.PassiveConditionKind;
import org.wx.core.wxBusiness.game.entity.enums.PassiveConditionType;
import org.wx.core.wxBusiness.game.entity.enums.PassiveEffectKind;
import org.wx.core.wxBusiness.game.entity.enums.PassiveEffectType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HeroPassiveService {

    @Resource
    private GameItemPassiveService itemPassiveService;
    @Resource
    private GamePassiveSkillService passiveSkillService;
    @Resource
    private SkillJsonHelper skillJsonHelper;
    @Resource
    private PlayerSkillDisplayHelper playerSkillDisplayHelper;

    public void applyPassives(HeroCombatService.HeroCombatContext ctx, GameHeroEquip equip,
                              Map<String, GameItem> itemMap) {
        if (ctx == null || equip == null) {
            return;
        }
        Set<String> equippedIds = ctx.getEquippedItemIds();
        List<GamePassiveSkill> passives = collectActivePassives(equip, equippedIds);
        if (passives.isEmpty()) {
            ctx.setPassiveActionValueFactor(BigDecimal.ONE);
            return;
        }

        int flatAttack = 0;
        int flatDefense = 0;
        int flatHp = 0;
        BigDecimal attackMul = BigDecimal.ONE;
        BigDecimal defenseMul = BigDecimal.ONE;
        BigDecimal hpMul = BigDecimal.ONE;
        BigDecimal actionFactor = BigDecimal.ONE;

        for (GamePassiveSkill passive : passives) {
            List<org.wx.core.wxBusiness.game.entity.skill.PassiveEffectVo> effects =
                    skillJsonHelper.readPassiveEffects(passive.getEffectsJson());
            if (!effects.isEmpty()) {
                for (org.wx.core.wxBusiness.game.entity.skill.PassiveEffectVo effect : effects) {
                    PassiveEffectKind kind = PassiveEffectKind.parse(effect.getKind());
                    if (kind == null || !kind.isOutBattle() || kind.isFormula()) {
                        continue;
                    }
                    BigDecimal raw = effect.getValue() != null ? effect.getValue() : BigDecimal.ZERO;
                    int sign = effect.getSign() != null && effect.getSign() < 0 ? -1 : 1;
                    BigDecimal val = raw.multiply(BigDecimal.valueOf(sign));
                    String stat = effect.getStat() != null ? effect.getStat().toUpperCase() : "ATTACK";
                    switch (kind) {
                        case OUT_STAT_FLAT -> {
                            int flat = val.setScale(0, RoundingMode.CEILING).intValue();
                            if ("DEFENSE".equals(stat)) flatDefense += flat;
                            else if ("HP".equals(stat)) flatHp += flat;
                            else flatAttack += flat;
                        }
                        case OUT_STAT_MULT -> {
                            BigDecimal mul = pctMultiplier(val);
                            if ("DEFENSE".equals(stat)) defenseMul = defenseMul.multiply(mul);
                            else if ("HP".equals(stat)) hpMul = hpMul.multiply(mul);
                            else attackMul = attackMul.multiply(mul);
                        }
                        default -> {
                        }
                    }
                }
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

        int attackBase = ctx.getHeroAttack() + ctx.getEquipAttack();
        int defenseBase = ctx.getHeroDefense() + ctx.getEquipDefense();
        int hpBase = ctx.getTotalMaxHp();

        int totalAttack = applyFlatThenPct(attackBase + flatAttack, attackMul);
        int totalDefense = applyFlatThenPct(defenseBase + flatDefense, defenseMul);
        int totalMaxHp = applyFlatThenPct(hpBase + flatHp, hpMul);

        ctx.setPassiveFlatAttack(flatAttack);
        ctx.setPassiveFlatDefense(flatDefense);
        ctx.setPassiveFlatHp(flatHp);
        ctx.setAttackPctMultiplier(attackMul);
        ctx.setDefensePctMultiplier(defenseMul);
        ctx.setHpPctMultiplier(hpMul);
        ctx.setTotalAttack(totalAttack);
        ctx.setTotalDefense(totalDefense);
        ctx.setTotalMaxHp(totalMaxHp);
        ctx.setPassiveActionValueFactor(actionFactor.setScale(6, RoundingMode.HALF_UP));
        ctx.setNormalAttackDamage(HeroCombatService.calcNormalAttackDamage(totalAttack, ctx.getDamageRatio()));
    }

    public List<HeroPassiveDetailVo> buildHeroPassiveDetails(GameHeroEquip equip, Set<String> equippedIds,
                                                            Map<String, GameItem> itemMap) {
        if (equip == null) {
            return List.of();
        }
        Set<String> equipped = equippedIds != null ? equippedIds : Set.of();
        List<HeroPassiveDetailVo> result = new ArrayList<>();
        if (!equipped.isEmpty()) {
            for (GameItemPassive binding : itemPassiveService.listEnabledByItemIds(new ArrayList<>(equipped))) {
                GameItem item = itemMap != null ? itemMap.get(binding.getItemId()) : null;
                String source = item != null ? "装备·" + item.getName() : "装备";
                appendHeroPassiveDetail(result, binding.getPassiveSkillId(), equipped, source, itemMap);
            }
        }
        return result;
    }

    private void appendHeroPassiveDetail(List<HeroPassiveDetailVo> result, String passiveSkillId,
                                         Set<String> equippedIds, String sourceLabel,
                                         Map<String, GameItem> itemMap) {
        GamePassiveSkill passive = passiveSkillService.getById(passiveSkillId);
        if (passive == null || !Integer.valueOf(1).equals(passive.getEnabled())) {
            return;
        }
        HeroPassiveDetailVo vo = new HeroPassiveDetailVo();
        vo.setId(passive.getId());
        vo.setName(passive.getName());
        vo.setSourceLabel(sourceLabel);
        vo.setConditionLabel(buildConditionLabel(passive, itemMap));
        List<org.wx.core.wxBusiness.game.entity.skill.PassiveEffectVo> v2Effects =
                skillJsonHelper.readPassiveEffects(passive.getEffectsJson());
        String v2EffectText = playerSkillDisplayHelper.formatPassiveEffects(v2Effects);
        if (v2EffectText != null && !v2EffectText.isBlank()) {
            vo.setEffectTypeLabel(v2EffectText);
            vo.setEffectValue(null);
        } else {
            PassiveEffectType effectType = PassiveEffectType.parse(passive.getEffectType());
            if (effectType != null) {
                vo.setEffectTypeLabel(effectType.getLabel());
            }
            vo.setEffectValue(passive.getEffectValue());
        }
        vo.setActive(matchesCondition(passive, equippedIds));
        result.add(vo);
    }

    private String buildConditionLabel(GamePassiveSkill passive, Map<String, GameItem> itemMap) {
        List<org.wx.core.wxBusiness.game.entity.skill.PassiveConditionVo> v2Conditions =
                skillJsonHelper.readPassiveConditions(passive.getConditionsJson());
        if (!v2Conditions.isEmpty()) {
            return playerSkillDisplayHelper.formatPassiveConditions(v2Conditions);
        }
        PassiveConditionType conditionType = PassiveConditionType.parse(passive.getConditionType());
        if (conditionType == null || conditionType == PassiveConditionType.NONE) {
            return "无条件";
        }
        if (conditionType == PassiveConditionType.REQUIRE_EQUIP) {
            String requiredItemId = passive.getConditionEquipItemId();
            if (requiredItemId == null || requiredItemId.isBlank()) {
                return conditionType.getLabel();
            }
            GameItem required = itemMap != null ? itemMap.get(requiredItemId) : null;
            String name = required != null ? required.getName() : requiredItemId;
            return "需装备「" + name + "」";
        }
        return conditionType.getLabel();
    }

    private List<GamePassiveSkill> collectActivePassives(GameHeroEquip equip, Set<String> equippedIds) {
        Map<String, GamePassiveSkill> deduped = new LinkedHashMap<>();
        collectFromEquippedItems(equippedIds, deduped);
        return new ArrayList<>(deduped.values());
    }

    private void collectFromEquippedItems(Set<String> equippedIds, Map<String, GamePassiveSkill> out) {
        if (equippedIds == null || equippedIds.isEmpty()) {
            return;
        }
        for (GameItemPassive binding : itemPassiveService.listEnabledByItemIds(new ArrayList<>(equippedIds))) {
            addPassiveIfActive(binding.getPassiveSkillId(), equippedIds, out);
        }
    }

    private void addPassiveIfActive(String passiveSkillId, Set<String> equippedIds,
                                    Map<String, GamePassiveSkill> out) {
        if (passiveSkillId == null || passiveSkillId.isBlank() || out.containsKey(passiveSkillId)) {
            return;
        }
        GamePassiveSkill passive = passiveSkillService.getById(passiveSkillId);
        if (passive == null || !Integer.valueOf(1).equals(passive.getEnabled())) {
            return;
        }
        if (!matchesCondition(passive, equippedIds)) {
            return;
        }
        out.put(passive.getId(), passive);
    }

    private boolean matchesCondition(GamePassiveSkill passive, Set<String> equippedIds) {
        List<org.wx.core.wxBusiness.game.entity.skill.PassiveConditionVo> conditions =
                skillJsonHelper.readPassiveConditions(passive.getConditionsJson());
        if (!conditions.isEmpty()) {
            for (org.wx.core.wxBusiness.game.entity.skill.PassiveConditionVo c : conditions) {
                PassiveConditionKind kind = PassiveConditionKind.parse(c.getType());
                if (kind == PassiveConditionKind.NONE) {
                    continue;
                }
                if (kind == PassiveConditionKind.REQUIRE_EQUIP) {
                    if (c.getEquipItemId() == null || c.getEquipItemId().isBlank()
                            || equippedIds == null || !equippedIds.contains(c.getEquipItemId())) {
                        return false;
                    }
                }
            }
            return true;
        }
        PassiveConditionType conditionType = PassiveConditionType.parse(passive.getConditionType());
        if (conditionType == null || conditionType == PassiveConditionType.NONE) {
            return true;
        }
        if (conditionType == PassiveConditionType.REQUIRE_EQUIP) {
            String requiredItemId = passive.getConditionEquipItemId();
            return requiredItemId != null && !requiredItemId.isBlank()
                    && equippedIds != null && equippedIds.contains(requiredItemId);
        }
        return false;
    }

    private BigDecimal pctMultiplier(BigDecimal percent) {
        return BigDecimal.ONE.add(percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }

    private BigDecimal reducePctMultiplier(BigDecimal percent) {
        BigDecimal factor = BigDecimal.ONE.subtract(percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return factor;
    }

    private int applyFlatThenPct(int baseWithFlat, BigDecimal multiplier) {
        return BigDecimal.valueOf(baseWithFlat)
                .multiply(multiplier)
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }
}
