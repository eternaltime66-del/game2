package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.BattleLog;
import org.wx.core.wxBusiness.game.entity.GameArmor;
import org.wx.core.wxBusiness.game.entity.GameFinishedSkill;
import org.wx.core.wxBusiness.game.entity.GameFinishedSkillEffect;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameItemPassive;
import org.wx.core.wxBusiness.game.entity.GamePassiveSkill;
import org.wx.core.wxBusiness.game.entity.GameRecipe;
import org.wx.core.wxBusiness.game.entity.GameRecipeMaterial;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;
import org.wx.core.wxBusiness.game.entity.GameWeapon;
import org.wx.core.wxBusiness.game.entity.ItemCraftPreviewVo;
import org.wx.core.wxBusiness.game.entity.ItemDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemFinishedSkillDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemPassiveDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemSkillEffectDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemTagHelper;
import org.wx.core.wxBusiness.game.entity.ItemTriggerSlotDetailVo;
import org.wx.core.wxBusiness.game.entity.enums.AdvancedEffectKind;
import org.wx.core.wxBusiness.game.entity.enums.EffectOutcomeType;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL1;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL2;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL4;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.entity.enums.PassiveConditionType;
import org.wx.core.wxBusiness.game.entity.enums.PassiveEffectType;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadResolver;
import org.wx.core.wxBusiness.game.entity.enums.SkillTargetType;
import org.wx.core.wxBusiness.game.entity.enums.StatRefType;
import org.wx.core.wxBusiness.game.entity.enums.TriggerSlotKind;
import org.wx.core.wxBusiness.game.entity.enums.TriggerSlotType;
import org.wx.core.wxBusiness.game.mapper.GameItemMapper;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameItemService extends WxServiceImpl<GameItemMapper, GameItem> {

    @Resource
    private GameWeaponService gameWeaponService;
    @Resource
    private GameArmorService gameArmorService;
    @Resource
    private GameRecipeMaterialService recipeMaterialService;
    @Resource
    private GameRecipeService recipeService;
    @Resource
    private GameTriggerSlotService triggerSlotService;
    @Resource
    private GameFinishedSkillService finishedSkillService;
    @Resource
    private GameFinishedSkillEffectService finishedSkillEffectService;

    @Resource
    private PlayerSkillDisplayHelper playerSkillDisplayHelper;
    @Resource
    private SkillJsonHelper skillJsonHelper;
    @Resource
    private GameItemPassiveService itemPassiveService;
    @Resource
    private GamePassiveSkillService passiveSkillService;

    public ItemDetailVo getItemDetail(String itemId) {
        GameItem item = this.getById(itemId);
        ErrorFactory.notNull(item, "物品不存在");
        ErrorFactory.throwError(!Integer.valueOf(1).equals(item.getEnabled()), "物品未启用");

        ItemDetailVo vo = new ItemDetailVo();
        vo.setId(item.getId());
        vo.setCode(item.getCode());
        vo.setName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setMaxStack(item.getMaxStack());
        vo.setWeight(item.getWeight());
        List<String> codes = new ArrayList<>(GameItemTag.toCodes(item.getItemTags()));
        vo.setItemTagCodes(codes);
        vo.setTags(new ArrayList<>(GameItemTag.toLabels(item.getItemTags())));
        vo.setItemTag(codes.isEmpty() ? null : codes.get(0));
        if (ItemTagHelper.hasTag(item, GameItemTag.WEAPON)) {
            GameWeapon weapon = gameWeaponService.getByItemId(itemId);
            if (weapon != null) {
                vo.setWeaponAttack(weapon.getAttack());
                vo.setWeaponBaseActionValue(weapon.getBaseActionValue());
                vo.setWeaponDamageRatio(weapon.getDamageRatio());
            }
        }
        GameArmor armor = gameArmorService.getByItemId(itemId);
        if (armor != null) {
            vo.setArmorBonusHp(armor.getBonusHp());
            vo.setArmorDefense(armor.getDefense());
            vo.setArmorBonusAttack(armor.getBonusAttack());
        }
        vo.setCrafts(listCraftPreviewsByMaterial(itemId));
        vo.setBasicAttackSlot(findSlotDetailByKind(itemId, TriggerSlotKind.BASIC_ATTACK));
        vo.setUltimateSlot(findSlotDetailByKind(itemId, TriggerSlotKind.ULTIMATE));
        vo.setTriggerSlots(listTraitActiveSlotDetails(itemId));
        vo.setPassiveSkills(listPassiveDetails(item, itemId));
        vo.setRemark(item.getRemark());
        return vo;
    }

    public ItemFinishedSkillDetailVo getFinishedSkillDetail(String finishedSkillId) {
        if (finishedSkillId == null || finishedSkillId.isBlank()) {
            return null;
        }
        return buildFinishedSkillDetail(finishedSkillId);
    }

    public ItemTriggerSlotDetailVo buildTriggerSlotDetailVo(GameTriggerSlot slot) {
        if (slot == null) {
            return null;
        }
        return buildTriggerSlotDetail(slot);
    }

    private List<ItemPassiveDetailVo> listPassiveDetails(GameItem item, String itemId) {
        List<ItemPassiveDetailVo> list = new ArrayList<>();
        for (GameItemPassive binding : itemPassiveService.listByItemId(itemId)) {
            if (!Integer.valueOf(1).equals(binding.getEnabled())) {
                continue;
            }
            ItemPassiveDetailVo detail = toPassiveDetail(binding.getPassiveSkillId());
            if (detail != null) {
                list.add(detail);
            }
        }
        return list;
    }

    private ItemPassiveDetailVo toPassiveDetail(String passiveSkillId) {
        GamePassiveSkill passive = passiveSkillService.getById(passiveSkillId);
        if (passive == null || !Integer.valueOf(1).equals(passive.getEnabled())) {
            return null;
        }
        ItemPassiveDetailVo vo = new ItemPassiveDetailVo();
        vo.setId(passive.getId());
        vo.setName(passive.getName());
        vo.setConditionLabel(buildPassiveConditionLabel(passive));
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
        vo.setRemark(passive.getRemark());
        return vo;
    }

    private String buildPassiveConditionLabel(GamePassiveSkill passive) {
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
            GameItem required = this.getById(requiredItemId);
            String name = required != null ? required.getName() : requiredItemId;
            return "需装备「" + name + "」";
        }
        return conditionType.getLabel();
    }

    private List<ItemTriggerSlotDetailVo> listTraitActiveSlotDetails(String itemId) {
        return triggerSlotService.listByItemId(itemId).stream()
                .filter(slot -> Integer.valueOf(1).equals(slot.getEnabled()))
                .filter(TriggerSlotKind::isTraitActive)
                .map(this::buildTriggerSlotDetail)
                .collect(Collectors.toList());
    }

    private ItemTriggerSlotDetailVo findSlotDetailByKind(String itemId, TriggerSlotKind kind) {
        GameTriggerSlot slot = switch (kind) {
            case BASIC_ATTACK -> triggerSlotService.findBasicAttackSlot(itemId);
            case ULTIMATE -> triggerSlotService.findUltimateSlot(itemId);
            default -> null;
        };
        if (slot == null || !Integer.valueOf(1).equals(slot.getEnabled())) {
            return null;
        }
        if (kind == TriggerSlotKind.BASIC_ATTACK) {
            if (slot.getFinishedSkillId() == null || slot.getFinishedSkillId().isBlank()) {
                return null;
            }
        } else if (kind == TriggerSlotKind.ULTIMATE) {
            if (slot.getFinishedSkillId() == null || slot.getFinishedSkillId().isBlank()) {
                return null;
            }
        }
        return buildTriggerSlotDetail(slot);
    }

    private ItemTriggerSlotDetailVo buildTriggerSlotDetail(GameTriggerSlot slot) {
        ItemTriggerSlotDetailVo vo = new ItemTriggerSlotDetailVo();
        vo.setId(slot.getId());
        vo.setTriggerSlotType(slot.getTriggerSlotType());
        TriggerSlotType slotType = TriggerSlotType.parse(slot.getTriggerSlotType());
        if (slotType != null) {
            vo.setTriggerSlotTypeLabel(slotType.getLabel());
        }
        TriggerSlotKind slotKind = resolveSlotKind(slot);
        vo.setSlotKind(slotKind.name());
        vo.setSlotKindLabel(slotKind.getLabel());
        vo.setTriggerParam(slot.getTriggerParam());
        vo.setTriggerRefId(slot.getTriggerRefId());
        if (slot.getTriggerRefId() != null && !slot.getTriggerRefId().isBlank()) {
            GameFinishedSkill refSkill = finishedSkillService.getById(slot.getTriggerRefId());
            if (refSkill != null) {
                vo.setTriggerRefName(BattleLog.buildSkillDisplayLabel(refSkill));
            }
        }
        if (slotKind == TriggerSlotKind.BASIC_ATTACK) {
            vo.setTriggerDesc(TriggerSlotType.ACTION_VALUE_FULL.getLabel());
        } else {
            String v2Desc = playerSkillDisplayHelper.formatSlotTriggerDesc(slot, vo.getTriggerRefName());
            if (v2Desc != null && !v2Desc.isBlank()) {
                vo.setTriggerDesc(v2Desc);
            } else {
                vo.setTriggerDesc(formatTriggerDesc(slot, vo.getTriggerRefName()));
            }
        }
        vo.setMaxCastCount(slot.getMaxCastCount());
        Integer skillMaxCast = null;
        if (slot.getFinishedSkillId() != null && !slot.getFinishedSkillId().isBlank()) {
            GameFinishedSkill linked = finishedSkillService.getById(slot.getFinishedSkillId());
            if (linked != null) {
                skillMaxCast = linked.getMaxCastCount();
            }
        }
        vo.setCastLimitText(playerSkillDisplayHelper.formatCastLimit(slot.getMaxCastCount(), skillMaxCast));
        if (slot.getFinishedSkillId() != null && !slot.getFinishedSkillId().isBlank()) {
            vo.setFinishedSkill(buildFinishedSkillDetail(slot.getFinishedSkillId()));
        }
        return vo;
    }

    private ItemFinishedSkillDetailVo buildFinishedSkillDetail(String finishedSkillId) {
        GameFinishedSkill skill = finishedSkillService.getById(finishedSkillId);
        if (skill == null) {
            return null;
        }
        ItemFinishedSkillDetailVo vo = new ItemFinishedSkillDetailVo();
        vo.setId(skill.getId());
        vo.setCode(skill.getCode());
        vo.setName(skill.getName());
        vo.setTargetType(skill.getTargetType());
        SkillTargetType targetType = SkillTargetType.parse(skill.getTargetType());
        if (targetType != null) {
            vo.setTargetLabel(formatTargetLabel(targetType, skill.getTargetParam()));
        }
        vo.setTargetParam(skill.getTargetParam());
        vo.setCategoryLabel(buildSkillCategoryLabel(skill));
        vo.setRemark(skill.getRemark());
        vo.setHitFrequency(skill.getHitFrequency());
        vo.setMaxCastCount(skill.getMaxCastCount());
        vo.setCastLimitText(playerSkillDisplayHelper.formatCastLimit(null, skill.getMaxCastCount()));
        for (GameFinishedSkillEffect effect : finishedSkillEffectService.listByFinishedSkillId(finishedSkillId)) {
            ItemSkillEffectDetailVo effectVo = buildEffectDetail(effect);
            int frequency = skill.getHitFrequency() != null && skill.getHitFrequency() > 0
                    ? skill.getHitFrequency() : 1;
            effectVo.setHitFrequency(frequency);
            effectVo.setTargetType(skill.getTargetType());
            effectVo.setTargetParam(skill.getTargetParam());
            if (targetType != null) {
                effectVo.setTargetLabel(formatTargetLabel(targetType, skill.getTargetParam()));
            }
            vo.getEffects().add(effectVo);
        }
        if (vo.getEffects().isEmpty()) {
            vo.getEffects().addAll(playerSkillDisplayHelper.buildEffectsFromFormulas(skill));
        }
        return vo;
    }

    private TriggerSlotKind resolveSlotKind(GameTriggerSlot slot) {
        if (TriggerSlotKind.isBasicAttack(slot)) {
            return TriggerSlotKind.BASIC_ATTACK;
        }
        if (TriggerSlotKind.isUltimate(slot)) {
            return TriggerSlotKind.ULTIMATE;
        }
        return TriggerSlotKind.TRAIT_ACTIVE;
    }

    private String buildSkillCategoryLabel(GameFinishedSkill skill) {
        FinishedSkillCatL1 c1 = FinishedSkillCatL1.parse(skill.getCatL1());
        FinishedSkillCatL2 c2 = FinishedSkillCatL2.parse(skill.getCatL2());
        FinishedSkillCatL4 c4 = FinishedSkillCatL4.parse(skill.getCatL4());
        String catL3 = skill.getCatL3() != null && !skill.getCatL3().isBlank() ? skill.getCatL3() : "通用";
        return c1.getLabel() + " · " + c2.getLabel() + " · " + catL3 + " · " + c4.getLabel();
    }

    private ItemSkillEffectDetailVo buildEffectDetail(GameFinishedSkillEffect effect) {
        ItemSkillEffectDetailVo vo = new ItemSkillEffectDetailVo();
        vo.setEffectKind(effect.getEffectKind());
        AdvancedEffectKind kind = AdvancedEffectKind.parse(effect.getEffectKind());
        if (kind != null) {
            vo.setEffectKindLabel(kind.getLabel());
        }
        vo.setOutcomeType(effect.getOutcomeType());
        EffectOutcomeType outcome = EffectOutcomeType.parse(effect.getOutcomeType());
        if (outcome != null) {
            vo.setOutcomeLabel(outcome.getLabel());
        }
        vo.setStatRef(effect.getStatRef());
        StatRefType statRef = StatRefType.parse(effect.getStatRef());
        if (statRef != null) {
            vo.setStatRefLabel(statRef.getLabel());
        } else if (effect.getStatRef() != null && !effect.getStatRef().isBlank()) {
            vo.setStatRefLabel(SkillReadResolver.resolveLabel(effect.getStatRef()));
        }
        vo.setRatioY(effect.getRatioY());
        vo.setUseWeaponRatio(effect.getUseWeaponRatio());
        vo.setFixedValue(effect.getFixedValue());
        vo.setActionDelta(effect.getActionDelta());
        vo.setSort(effect.getSort());
        vo.setFormulaText(buildFormulaText(effect));
        return vo;
    }

    private String formatTriggerDesc(GameTriggerSlot slot, String triggerRefName) {
        TriggerSlotType type = TriggerSlotType.parse(slot.getTriggerSlotType());
        if (type == null) {
            return slot.getTriggerSlotType();
        }
        if (!type.isNeedParam()) {
            return type.getLabel();
        }
        int x = slot.getTriggerParam() != null ? slot.getTriggerParam().intValue() : 0;
        if (type == TriggerSlotType.FINISHED_SKILL_CAST_COUNT) {
            String ref = triggerRefName != null ? triggerRefName : "指定技能";
            return "释放「" + ref + "」" + x + " 次后";
        }
        return type.getLabel().replace("x", String.valueOf(x));
    }

    private String formatCastLimit(Integer maxCastCount) {
        if (maxCastCount == null || maxCastCount <= 0) {
            return "无限";
        }
        return "最多 " + maxCastCount + " 次/场";
    }

    private String formatTargetLabel(SkillTargetType targetType, Integer targetParam) {
        String label = targetType.getLabel();
        if (label.contains("x") && targetParam != null) {
            return label.replace("x", String.valueOf(targetParam));
        }
        return label;
    }

    private String buildFormulaText(GameFinishedSkillEffect effect) {
        AdvancedEffectKind kind = AdvancedEffectKind.parse(effect.getEffectKind());
        if (kind == AdvancedEffectKind.ACTION_VALUE) {
            int delta = effect.getActionDelta() != null ? effect.getActionDelta() : 0;
            return "行动值 " + (delta >= 0 ? "+" : "") + delta;
        }
        if (kind == AdvancedEffectKind.FIXED_VALUE) {
            EffectOutcomeType outcome = EffectOutcomeType.parse(effect.getOutcomeType());
            String prefix = outcome == EffectOutcomeType.HEAL ? "治疗 " : "伤害 ";
            BigDecimal val = effect.getFixedValue() != null ? effect.getFixedValue() : BigDecimal.ZERO;
            return prefix + val.stripTrailingZeros().toPlainString();
        }
        StatRefType statRef = StatRefType.parse(effect.getStatRef());
        String statLabel = statRef != null ? statRef.getLabel()
                : SkillReadResolver.resolveLabel(effect.getStatRef());
        if (statLabel == null || statLabel.isBlank()) {
            statLabel = "属性";
        }
        BigDecimal y = effect.getRatioY() != null ? effect.getRatioY() : BigDecimal.ONE;
        String formula = statLabel + " × " + y.stripTrailingZeros().toPlainString();
        if (Integer.valueOf(1).equals(effect.getUseWeaponRatio())) {
            formula += " × 武器伤害比例";
        }
        EffectOutcomeType outcome = EffectOutcomeType.parse(effect.getOutcomeType());
        if (outcome == EffectOutcomeType.HEAL) {
            return "治疗：" + formula;
        }
        return "伤害：" + formula;
    }

    private List<ItemCraftPreviewVo> listCraftPreviewsByMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        List<GameRecipeMaterial> links = recipeMaterialService.find()
                .eq(GameRecipeMaterial::getMaterialItemId, itemId)
                .orderByAsc(GameRecipeMaterial::getSort)
                .list();
        if (links.isEmpty()) {
            return List.of();
        }
        Set<String> recipeIds = links.stream()
                .map(GameRecipeMaterial::getRecipeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ItemCraftPreviewVo> previews = new ArrayList<>();
        for (String recipeId : recipeIds) {
            GameRecipe recipe = recipeService.getById(recipeId);
            if (recipe == null || !Integer.valueOf(1).equals(recipe.getEnabled())) {
                continue;
            }
            GameItem result = this.getById(recipe.getOutputItemId());
            ItemCraftPreviewVo vo = new ItemCraftPreviewVo();
            vo.setCraftId(recipe.getId());
            vo.setName(result != null ? result.getName() : recipe.getId());
            vo.setRemark(recipe.getRemark());
            vo.setResultItemId(recipe.getOutputItemId());
            vo.setResultItemName(result != null ? result.getName() : recipe.getId());
            previews.add(vo);
        }
        return previews;
    }
}
