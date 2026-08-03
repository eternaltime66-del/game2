package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.GameArmor;
import org.wx.core.wxBusiness.game.entity.GameFinishedSkill;
import org.wx.core.wxBusiness.game.entity.GameFinishedSkillEffect;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameRecipe;
import org.wx.core.wxBusiness.game.entity.GameRecipeMaterial;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;
import org.wx.core.wxBusiness.game.entity.GameWeapon;
import org.wx.core.wxBusiness.game.entity.ItemCraftPreviewVo;
import org.wx.core.wxBusiness.game.entity.ItemDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemFinishedSkillDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemSkillEffectDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemTagHelper;
import org.wx.core.wxBusiness.game.entity.ItemTriggerSlotDetailVo;
import org.wx.core.wxBusiness.game.entity.enums.AdvancedEffectKind;
import org.wx.core.wxBusiness.game.entity.enums.EffectOutcomeType;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.entity.enums.SkillTargetType;
import org.wx.core.wxBusiness.game.entity.enums.StatRefType;
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
        if (ItemTagHelper.hasTag(item, GameItemTag.ARMOR)) {
            GameArmor armor = gameArmorService.getByItemId(itemId);
            if (armor != null) {
                vo.setArmorBonusHp(armor.getBonusHp());
                vo.setArmorDefense(armor.getDefense());
            }
        }
        vo.setCrafts(listCraftPreviewsByMaterial(itemId));
        vo.setTriggerSlots(listTriggerSlotDetails(itemId));
        vo.setRemark(item.getRemark());
        return vo;
    }

    private List<ItemTriggerSlotDetailVo> listTriggerSlotDetails(String itemId) {
        return triggerSlotService.listByItemId(itemId).stream()
                .filter(slot -> Integer.valueOf(1).equals(slot.getEnabled()))
                .map(this::buildTriggerSlotDetail)
                .collect(Collectors.toList());
    }

    private ItemTriggerSlotDetailVo buildTriggerSlotDetail(GameTriggerSlot slot) {
        ItemTriggerSlotDetailVo vo = new ItemTriggerSlotDetailVo();
        vo.setId(slot.getId());
        vo.setTriggerSlotType(slot.getTriggerSlotType());
        TriggerSlotType slotType = TriggerSlotType.parse(slot.getTriggerSlotType());
        if (slotType != null) {
            vo.setTriggerSlotTypeLabel(slotType.getLabel());
        }
        vo.setTriggerParam(slot.getTriggerParam());
        vo.setTriggerRefId(slot.getTriggerRefId());
        if (slot.getTriggerRefId() != null && !slot.getTriggerRefId().isBlank()) {
            GameFinishedSkill refSkill = finishedSkillService.getById(slot.getTriggerRefId());
            if (refSkill != null) {
                vo.setTriggerRefName(refSkill.getName());
            }
        }
        vo.setTriggerDesc(formatTriggerDesc(slot, vo.getTriggerRefName()));
        vo.setMaxCastCount(slot.getMaxCastCount());
        vo.setCastLimitText(formatCastLimit(slot.getMaxCastCount()));
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
        vo.setRemark(skill.getRemark());
        for (GameFinishedSkillEffect effect : finishedSkillEffectService.listByFinishedSkillId(finishedSkillId)) {
            vo.getEffects().add(buildEffectDetail(effect));
        }
        return vo;
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
            return "每释放「" + ref + "」" + x + " 次";
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
        String statLabel = statRef != null ? statRef.getLabel() : "属性";
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
