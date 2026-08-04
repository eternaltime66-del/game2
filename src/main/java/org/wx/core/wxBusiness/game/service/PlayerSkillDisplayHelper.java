package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.wx.core.wxBusiness.game.entity.GameFinishedSkill;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;
import org.wx.core.wxBusiness.game.entity.ItemSkillEffectDetailVo;
import org.wx.core.wxBusiness.game.entity.enums.AdvancedEffectKind;
import org.wx.core.wxBusiness.game.entity.enums.EffectOutcomeType;
import org.wx.core.wxBusiness.game.entity.enums.PassiveConditionKind;
import org.wx.core.wxBusiness.game.entity.enums.PassiveEffectKind;
import org.wx.core.wxBusiness.game.entity.enums.SkillCompareOp;
import org.wx.core.wxBusiness.game.entity.enums.SkillFormulaOutcome;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadResolver;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadType;
import org.wx.core.wxBusiness.game.entity.enums.SkillScopeFilter;
import org.wx.core.wxBusiness.game.entity.enums.SkillTargetType;
import org.wx.core.wxBusiness.game.entity.enums.StatRefType;
import org.wx.core.wxBusiness.game.entity.skill.PassiveConditionVo;
import org.wx.core.wxBusiness.game.entity.skill.PassiveEffectVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionItemVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaTokenVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 玩家端技能/扳机展示文案（v2 formulasJson / conditionsJson → 可读描述）
 */
@Component
public class PlayerSkillDisplayHelper {

    @Resource
    private SkillJsonHelper skillJsonHelper;

    public List<ItemSkillEffectDetailVo> buildEffectsFromFormulas(GameFinishedSkill skill) {
        List<SkillFormulaGroupVo> formulas = skillJsonHelper.readFormulas(skill.getFormulasJson());
        if (formulas.isEmpty()) {
            return List.of();
        }
        List<ItemSkillEffectDetailVo> effects = new ArrayList<>();
        int sort = 0;
        for (SkillFormulaGroupVo formula : formulas) {
            if (formula == null) {
                continue;
            }
            ItemSkillEffectDetailVo vo = new ItemSkillEffectDetailVo();
            SkillFormulaOutcome outcome = SkillFormulaOutcome.parse(formula.getOutcome());
            AdvancedEffectKind effectKind = resolveFormulaEffectKind(outcome);
            vo.setEffectKind(effectKind.name());
            vo.setEffectKindLabel(effectKind.getLabel());
            if (outcome == SkillFormulaOutcome.DAMAGE || outcome == SkillFormulaOutcome.HEAL) {
                EffectOutcomeType legacy = EffectOutcomeType.parse(outcome.name());
                if (legacy != null) {
                    vo.setOutcomeType(legacy.name());
                    vo.setOutcomeLabel(legacy.getLabel());
                }
            } else if (outcome != null) {
                vo.setOutcomeType(outcome.name());
                vo.setOutcomeLabel(outcome.getLabel());
            }
            vo.setFormulaText(buildFormulaText(formula));
            applySimpleFormulaMeta(formula, vo, effectKind);
            int frequency = formula.getHitFrequency() != null && formula.getHitFrequency() > 0
                    ? formula.getHitFrequency() : 1;
            vo.setHitFrequency(frequency);
            applyFormulaTarget(formula, skill, vo);
            vo.setSort(sort++);
            effects.add(vo);
        }
        return effects;
    }

    public String formatPassiveConditions(List<PassiveConditionVo> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "无条件";
        }
        List<String> parts = new ArrayList<>();
        for (PassiveConditionVo condition : conditions) {
            if (condition == null) {
                continue;
            }
            PassiveConditionKind kind = PassiveConditionKind.parse(condition.getType());
            if (kind == null || kind == PassiveConditionKind.NONE) {
                continue;
            }
            if (kind == PassiveConditionKind.REQUIRE_EQUIP) {
                parts.add("需装备指定物品");
            } else if (kind == PassiveConditionKind.COMPARE) {
                String left = formatOperand(null, condition.getLeftKind(), condition.getLeftRead(),
                        condition.getLeftFilter(), condition.getLeftFilterRef(), condition.getLeftConst(), null);
                String right = formatOperand(null, condition.getRightKind(), condition.getRightRead(),
                        condition.getRightFilter(), condition.getRightFilterRef(), condition.getRightConst(), null);
                SkillCompareOp op = SkillCompareOp.parse(condition.getOp());
                String opText = op != null ? op.getSymbol() : (condition.getOp() != null ? condition.getOp() : "?");
                parts.add(left + " " + opText + " " + right);
            } else {
                parts.add(kind.getLabel());
            }
        }
        return parts.isEmpty() ? "无条件" : String.join("；", parts);
    }

    public String formatPassiveEffects(List<PassiveEffectVo> effects) {
        if (effects == null || effects.isEmpty()) {
            return null;
        }
        return effects.stream()
                .map(this::formatPassiveEffect)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("；"));
    }

    private String formatPassiveEffect(PassiveEffectVo effect) {
        if (effect == null) {
            return null;
        }
        PassiveEffectKind kind = PassiveEffectKind.parse(effect.getKind());
        String kindLabel = kind != null ? kind.getLabel() : effect.getKind();
        if (kind != null && kind.isFormula()) {
            String expr = formatFormulaTokens(effect.getTokens());
            return kindLabel + (expr.isBlank() ? "" : "：" + expr);
        }
        String statLabel = SkillReadResolver.resolveLabel(effect.getStat());
        if (statLabel == null || statLabel.isBlank()) {
            statLabel = effect.getStat() != null ? effect.getStat() : "";
        }
        int sign = effect.getSign() != null && effect.getSign() < 0 ? -1 : 1;
        BigDecimal raw = effect.getValue() != null ? effect.getValue() : BigDecimal.ZERO;
        BigDecimal signed = raw.multiply(BigDecimal.valueOf(sign));
        String valText = signed.stripTrailingZeros().toPlainString();
        if (statLabel.isBlank()) {
            return kindLabel + " " + valText;
        }
        return kindLabel + " · " + statLabel + " " + (sign < 0 ? "" : "+") + valText;
    }

    private String formatFormulaTokens(List<SkillFormulaTokenVo> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        return tokens.stream()
                .map(t -> formatToken(t, null))
                .collect(Collectors.joining(""))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private AdvancedEffectKind resolveFormulaEffectKind(SkillFormulaOutcome outcome) {
        if (outcome == SkillFormulaOutcome.ACTION_INC || outcome == SkillFormulaOutcome.ACTION_DEC) {
            return AdvancedEffectKind.ACTION_VALUE;
        }
        return AdvancedEffectKind.STAT_FORMULA;
    }

    public String formatSlotTriggerDesc(GameTriggerSlot slot, String triggerRefName) {
        if (slot == null) {
            return null;
        }
        SkillSlotConditionsVo conditions = skillJsonHelper.resolveSlotConditions(
                slot.getTriggerMode(), slot.getQuickPreset(), slot.getConditionsJson());
        if (conditions == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (conditions.getPrerequisites() != null) {
            conditions.getPrerequisites().forEach(p -> {
                if (p != null && p.getType() != null && !"NONE".equalsIgnoreCase(p.getType())) {
                    parts.add(formatPrerequisite(p, triggerRefName));
                }
            });
        }
        if (conditions.getConditionGroups() != null) {
            for (SkillConditionGroupVo group : conditions.getConditionGroups()) {
                String text = formatConditionGroup(group, triggerRefName);
                if (text != null && !text.isBlank()) {
                    parts.add(text);
                }
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("；", parts);
    }

    public String formatCastLimit(Integer slotMaxCast, Integer skillMaxCast) {
        Integer limit = slotMaxCast;
        if (limit == null || limit <= 0) {
            limit = skillMaxCast;
        }
        if (limit == null || limit <= 0) {
            return "无限";
        }
        return "最多 " + limit + " 次/场";
    }

    private String formatConditionGroup(SkillConditionGroupVo group, String triggerRefName) {
        if (group == null || group.getItems() == null || group.getItems().isEmpty()) {
            return null;
        }
        return group.getItems().stream()
                .map(item -> formatConditionItem(item, triggerRefName))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" 且 "));
    }

    private String formatConditionItem(SkillConditionItemVo item, String triggerRefName) {
        if (item == null) {
            return null;
        }
        String left = formatOperand(item.getLeftTokens(), item.getLeftKind(), item.getLeftRead(),
                item.getLeftFilter(), item.getLeftFilterRef(), item.getLeftConst(), triggerRefName);
        String right = formatOperand(item.getRightTokens(), item.getRightKind(), item.getRightRead(),
                item.getRightFilter(), item.getRightFilterRef(), item.getRightConst(), triggerRefName);
        SkillCompareOp op = SkillCompareOp.parse(item.getOp());
        if (op == SkillCompareOp.MOD) {
            return left + " 每满 " + right + " 次";
        }
        if (op == SkillCompareOp.EVERY) {
            return left + " 每累计达到 " + right;
        }
        String opText = op != null ? op.getSymbol() : (item.getOp() != null ? item.getOp() : "?");
        return left + " " + opText + " " + right;
    }

    private String formatOperand(List<SkillFormulaTokenVo> tokens, String kind, String read,
                                 String filter, String filterRef, BigDecimal constant,
                                 String triggerRefName) {
        if (tokens != null && !tokens.isEmpty()) {
            return tokens.stream()
                    .map(t -> formatToken(t, triggerRefName))
                    .collect(Collectors.joining(""));
        }
        if ("READ".equalsIgnoreCase(kind) && read != null) {
            return formatRead(read, filter, filterRef, triggerRefName);
        }
        if ("CONST".equalsIgnoreCase(kind)) {
            return formatNumber(constant);
        }
        return "?";
    }

    private String formatToken(SkillFormulaTokenVo token, String triggerRefName) {
        if (token == null || token.getKind() == null) {
            return "";
        }
        return switch (token.getKind().toUpperCase()) {
            case "READ" -> formatRead(token.getRead(), token.getFilter(), token.getFilterRef(), triggerRefName);
            case "CONST" -> formatNumber(token.getValue());
            case "OP" -> " " + (token.getOp() != null ? token.getOp() : "") + " ";
            case "LPAREN" -> "(";
            case "RPAREN" -> ")";
            default -> "";
        };
    }

    private String formatRead(String read, String filter, String filterRef, String triggerRefName) {
        SkillReadType type = SkillReadType.parse(read);
        String label = type != null ? type.getLabel() : read;
        SkillScopeFilter scope = SkillScopeFilter.parse(filter);
        if (scope != null) {
            if (scope.needsSkillRef() && filterRef != null && !filterRef.isBlank()) {
                label = label + "·" + (triggerRefName != null ? triggerRefName : filterRef);
            } else if (scope != SkillScopeFilter.ANY_SKILL && scope != SkillScopeFilter.ANY_TRIGGER) {
                label = scope.getLabel() + label;
            }
        }
        return label;
    }

    private String formatPrerequisite(org.wx.core.wxBusiness.game.entity.skill.SkillPrerequisiteVo p,
                                      String triggerRefName) {
        if (p == null || p.getType() == null) {
            return "";
        }
        return switch (p.getType()) {
            case "HOLD_WEAPON" -> "装备武器";
            case "HOLD_ARMOR" -> "装备护甲";
            case "HOLD_ITEM" -> "持有物品" + (p.getItemId() != null ? "·" + p.getItemId() : "");
            case "HOLD_PERSON_SKILL" -> "持有人物技能"
                    + (p.getFinishedSkillId() != null ? "·" + p.getFinishedSkillId() : "");
            default -> p.getType();
        };
    }

    private String buildFormulaText(SkillFormulaGroupVo formula) {
        SkillFormulaOutcome outcome = SkillFormulaOutcome.parse(formula.getOutcome());
        String prefix = outcome != null ? outcome.getLabel() + "：" : "";
        if (formula.getTokens() == null || formula.getTokens().isEmpty()) {
            return prefix + "—";
        }
        String expr = formula.getTokens().stream()
                .map(t -> formatToken(t, null))
                .collect(Collectors.joining(""))
                .replaceAll("\\s+", " ")
                .trim();
        return prefix + expr;
    }

    private void applySimpleFormulaMeta(SkillFormulaGroupVo formula, ItemSkillEffectDetailVo vo,
                                        AdvancedEffectKind effectKind) {
        if (formula.getTokens() == null || formula.getTokens().isEmpty()) {
            return;
        }
        if (effectKind == AdvancedEffectKind.ACTION_VALUE && formula.getTokens().size() == 1) {
            SkillFormulaTokenVo only = formula.getTokens().get(0);
            if ("CONST".equalsIgnoreCase(only.getKind()) && only.getValue() != null) {
                vo.setActionDelta(only.getValue().intValue());
            }
        }
        if (formula.getTokens().size() < 3) {
            return;
        }
        SkillFormulaTokenVo read = formula.getTokens().get(0);
        SkillFormulaTokenVo op = formula.getTokens().get(1);
        SkillFormulaTokenVo val = formula.getTokens().get(2);
        if (!"READ".equalsIgnoreCase(read.getKind()) || !"*".equals(op.getOp())
                || !"CONST".equalsIgnoreCase(val.getKind())) {
            return;
        }
        String readCode = read.getRead();
        StatRefType stat = SkillReadResolver.toStatRef(readCode);
        if (stat != null) {
            vo.setStatRef(stat.name());
            vo.setStatRefLabel(stat.getLabel());
        } else if (readCode != null) {
            vo.setStatRef(readCode);
            vo.setStatRefLabel(SkillReadResolver.resolveLabel(readCode));
        }
        if (val.getValue() != null) {
            vo.setRatioY(val.getValue());
            if (effectKind == AdvancedEffectKind.ACTION_VALUE) {
                vo.setActionDelta(val.getValue().intValue());
            }
        }
        if (SkillReadResolver.usesWeaponRatio(readCode)) {
            vo.setUseWeaponRatio(1);
        }
    }

    private void applyFormulaTarget(SkillFormulaGroupVo formula, GameFinishedSkill skill, ItemSkillEffectDetailVo vo) {
        String targetTypeCode = formula.getTargetType();
        Integer targetParam = formula.getTargetParam();
        if (targetTypeCode == null || targetTypeCode.isBlank()) {
            if (skill == null) {
                return;
            }
            targetTypeCode = skill.getTargetType();
            targetParam = skill.getTargetParam();
        }
        if (targetTypeCode == null || targetTypeCode.isBlank()) {
            return;
        }
        vo.setTargetType(targetTypeCode);
        vo.setTargetParam(targetParam);
        SkillTargetType targetType = SkillTargetType.parse(targetTypeCode);
        if (targetType != null) {
            vo.setTargetLabel(formatTargetLabel(targetType, targetParam));
        } else {
            vo.setTargetLabel(targetTypeCode);
        }
    }

    private String formatTargetLabel(SkillTargetType targetType, Integer targetParam) {
        if (targetType == null) {
            return "";
        }
        String label = targetType.getLabel();
        if (label.contains("x") && targetParam != null) {
            return label.replace("x", String.valueOf(targetParam));
        }
        return label;
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
