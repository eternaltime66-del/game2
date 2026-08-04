package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Component;
import org.wx.core.wxBusiness.game.entity.BattleUnit;
import org.wx.core.wxBusiness.game.entity.enums.ConditionZoneMode;
import org.wx.core.wxBusiness.game.entity.enums.SkillCompareOp;
import org.wx.core.wxBusiness.game.entity.enums.SkillOperandKind;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadType;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionItemVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaTokenVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillPrerequisiteVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** 条件判定 + 公式求值 */
@Component
public class SkillExpressionService {

    @FunctionalInterface
    public interface SkillValueReader {
        BigDecimal read(SkillReadType type, String filter, String filterRef);
    }

    public boolean anyGroupMatch(List<SkillConditionGroupVo> groups, SkillValueReader reader) {
        return anyGroupMatch(groups, reader, null);
    }

    public boolean anyGroupMatch(List<SkillConditionGroupVo> groups, SkillValueReader reader,
                                 Predicate<SkillPrerequisiteVo> prerequisiteChecker) {
        // prerequisiteChecker 保留签名兼容；前置已在 slotMatch 层处理
        if (groups == null || groups.isEmpty()) {
            return true;
        }
        for (SkillConditionGroupVo group : groups) {
            if (groupMatch(group, reader, prerequisiteChecker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 槽位步进判定结果。
     * usesEvery=true 时 steps=⌊累计/阈值⌋（已跨过的档数），引擎用 steps-已触发 决定连发次数。
     */
    public record SlotStepMatch(boolean matched, boolean usesEvery, int steps) {
        public static SlotStepMatch miss() {
            return new SlotStepMatch(false, false, 0);
        }

        public static SlotStepMatch once() {
            return new SlotStepMatch(true, false, 1);
        }

        public static SlotStepMatch every(int steps) {
            return new SlotStepMatch(true, true, Math.max(0, steps));
        }
    }

    /** 槽位条件：前置区 AND 数值区 */
    public boolean slotMatch(SkillSlotConditionsVo slot, SkillValueReader reader,
                             Predicate<SkillPrerequisiteVo> prerequisiteChecker) {
        SlotStepMatch m = slotStepMatch(slot, reader, prerequisiteChecker);
        if (!m.matched()) {
            return false;
        }
        // 「每累计达到」在尚未跨过第 1 档时不算命中
        return !m.usesEvery() || m.steps() > 0;
    }

    /**
     * 槽位条件步进版：含「每累计达到」时返回已跨档数，供战斗引擎按档触发（小数伤害可用）。
     */
    public SlotStepMatch slotStepMatch(SkillSlotConditionsVo slot, SkillValueReader reader,
                                       Predicate<SkillPrerequisiteVo> prerequisiteChecker) {
        if (slot == null) {
            return SlotStepMatch.once();
        }
        if (!prerequisitesMatch(slot, prerequisiteChecker)) {
            return SlotStepMatch.miss();
        }

        ConditionZoneMode numericMode = ConditionZoneMode.parse(slot.getNumericMode());
        if (slot.getNumericMode() == null || slot.getNumericMode().isBlank()) {
            boolean hasItems = slot.getConditionGroups() != null && slot.getConditionGroups().stream()
                    .anyMatch(g -> g != null && g.getItems() != null && !g.getItems().isEmpty());
            numericMode = hasItems ? ConditionZoneMode.CONFIG : ConditionZoneMode.NONE;
        }
        if (numericMode != ConditionZoneMode.CONFIG) {
            return SlotStepMatch.once();
        }
        List<SkillConditionGroupVo> groups = slot.getConditionGroups();
        if (groups == null || groups.isEmpty()) {
            return SlotStepMatch.once();
        }

        SlotStepMatch bestEvery = null;
        boolean boolMatch = false;
        for (SkillConditionGroupVo group : groups) {
            GroupStepMatch g = numericGroupStepMatch(group, reader);
            if (!g.ok()) {
                continue;
            }
            if (g.usesEvery()) {
                if (bestEvery == null || g.steps() > bestEvery.steps()) {
                    bestEvery = SlotStepMatch.every(g.steps());
                }
            } else {
                boolMatch = true;
            }
        }
        if (bestEvery != null) {
            return bestEvery;
        }
        if (boolMatch) {
            return SlotStepMatch.once();
        }
        return SlotStepMatch.miss();
    }

    private boolean prerequisitesMatch(SkillSlotConditionsVo slot,
                                       Predicate<SkillPrerequisiteVo> prerequisiteChecker) {
        ConditionZoneMode prereqMode = ConditionZoneMode.parse(slot.getPrerequisiteMode());
        if (slot.getPrerequisiteMode() == null || slot.getPrerequisiteMode().isBlank()) {
            prereqMode = (slot.getPrerequisites() != null && !slot.getPrerequisites().isEmpty())
                    ? ConditionZoneMode.CONFIG : ConditionZoneMode.NONE;
        }
        if (prereqMode != ConditionZoneMode.CONFIG
                || slot.getPrerequisites() == null
                || slot.getPrerequisites().isEmpty()) {
            return true;
        }
        if (prerequisiteChecker == null) {
            return false;
        }
        for (SkillPrerequisiteVo p : slot.getPrerequisites()) {
            if (p == null || !prerequisiteChecker.test(p)) {
                return false;
            }
        }
        return true;
    }

    private record GroupStepMatch(boolean ok, boolean usesEvery, int steps) {
        static GroupStepMatch fail() {
            return new GroupStepMatch(false, false, 0);
        }
    }

    private GroupStepMatch numericGroupStepMatch(SkillConditionGroupVo group, SkillValueReader reader) {
        if (group == null) {
            return new GroupStepMatch(true, false, 1);
        }
        ConditionZoneMode numericMode = resolveNumericMode(group);
        if (numericMode == ConditionZoneMode.NONE) {
            return new GroupStepMatch(true, false, 1);
        }
        if (group.getItems() == null || group.getItems().isEmpty()) {
            return new GroupStepMatch(true, false, 1);
        }
        boolean usesEvery = false;
        int minSteps = Integer.MAX_VALUE;
        for (SkillConditionItemVo item : group.getItems()) {
            if (item == null || item.getOp() == null) {
                continue;
            }
            SkillCompareOp op = SkillCompareOp.parse(item.getOp());
            if (op == SkillCompareOp.EVERY) {
                BigDecimal left = resolveConditionSide(item.getLeftTokens(), item.getLeftKind(), item.getLeftRead(),
                        item.getLeftFilter(), item.getLeftFilterRef(), item.getLeftConst(), reader);
                BigDecimal right = resolveConditionSide(item.getRightTokens(), item.getRightKind(), item.getRightRead(),
                        item.getRightFilter(), item.getRightFilterRef(), item.getRightConst(), reader);
                if (left == null || right == null || right.compareTo(BigDecimal.ZERO) <= 0) {
                    return GroupStepMatch.fail();
                }
                usesEvery = true;
                minSteps = Math.min(minSteps, everySteps(left, right));
            } else if (!itemMatch(item, reader)) {
                return GroupStepMatch.fail();
            }
        }
        if (usesEvery) {
            return new GroupStepMatch(true, true, minSteps == Integer.MAX_VALUE ? 0 : minSteps);
        }
        return new GroupStepMatch(true, false, 1);
    }

    /** ⌊left / right⌋，阈值档数；right&lt;=0 或空时为 0 */
    public int everySteps(BigDecimal left, BigDecimal right) {
        if (left == null || right == null || right.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (left.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return left.divide(right, 0, RoundingMode.FLOOR).intValue();
    }

    public boolean groupMatch(SkillConditionGroupVo group, SkillValueReader reader) {
        return groupMatch(group, reader, null);
    }

    public boolean groupMatch(SkillConditionGroupVo group, SkillValueReader reader,
                              Predicate<SkillPrerequisiteVo> prerequisiteChecker) {
        if (group == null) {
            return true;
        }
        // 新模型：组只做数值；若旧数据组内仍带前置，一并 AND（兼容）
        if (!numericGroupMatch(group, reader)) {
            return false;
        }
        ConditionZoneMode prereqMode = resolvePrerequisiteMode(group);
        if (prereqMode != ConditionZoneMode.CONFIG
                || group.getPrerequisites() == null
                || group.getPrerequisites().isEmpty()) {
            return true;
        }
        if (prerequisiteChecker == null) {
            return false;
        }
        for (SkillPrerequisiteVo p : group.getPrerequisites()) {
            if (p == null || !prerequisiteChecker.test(p)) {
                return false;
            }
        }
        return true;
    }

    private boolean numericGroupMatch(SkillConditionGroupVo group, SkillValueReader reader) {
        if (group == null) {
            return true;
        }
        ConditionZoneMode numericMode = resolveNumericMode(group);
        if (numericMode == ConditionZoneMode.NONE) {
            return true;
        }
        if (group.getItems() == null || group.getItems().isEmpty()) {
            return true;
        }
        for (SkillConditionItemVo item : group.getItems()) {
            if (!itemMatch(item, reader)) {
                return false;
            }
        }
        return true;
    }

    private ConditionZoneMode resolveNumericMode(SkillConditionGroupVo group) {
        if (group.getNumericMode() == null || group.getNumericMode().isBlank()) {
            return (group.getItems() != null && !group.getItems().isEmpty())
                    ? ConditionZoneMode.CONFIG
                    : ConditionZoneMode.NONE;
        }
        return ConditionZoneMode.parse(group.getNumericMode());
    }

    private ConditionZoneMode resolvePrerequisiteMode(SkillConditionGroupVo group) {
        if (group.getPrerequisiteMode() == null || group.getPrerequisiteMode().isBlank()) {
            return (group.getPrerequisites() != null && !group.getPrerequisites().isEmpty())
                    ? ConditionZoneMode.CONFIG
                    : ConditionZoneMode.NONE;
        }
        return ConditionZoneMode.parse(group.getPrerequisiteMode());
    }

    public boolean itemMatch(SkillConditionItemVo item, SkillValueReader reader) {
        if (item == null || item.getOp() == null) {
            return true;
        }
        BigDecimal left = resolveConditionSide(item.getLeftTokens(), item.getLeftKind(), item.getLeftRead(),
                item.getLeftFilter(), item.getLeftFilterRef(), item.getLeftConst(), reader);
        BigDecimal right = resolveConditionSide(item.getRightTokens(), item.getRightKind(), item.getRightRead(),
                item.getRightFilter(), item.getRightFilterRef(), item.getRightConst(), reader);
        if (left == null || right == null) {
            return false;
        }
        SkillCompareOp op = SkillCompareOp.parse(item.getOp());
        if (op == null) {
            return false;
        }
        return switch (op) {
            case GT -> left.compareTo(right) > 0;
            case LT -> left.compareTo(right) < 0;
            case GTE -> left.compareTo(right) >= 0;
            case LTE -> left.compareTo(right) <= 0;
            case EQ -> left.compareTo(right) == 0;
            case MOD -> right.compareTo(BigDecimal.ZERO) != 0
                    && left.remainder(right).compareTo(BigDecimal.ZERO) == 0;
            // 布尔语义：至少跨过 1 档；多档连发由 slotStepMatch + 战斗计数处理
            case EVERY -> everySteps(left, right) > 0;
        };
    }

    /** 优先公式 token；无 token 时回退旧 READ/CONST */
    private BigDecimal resolveConditionSide(List<SkillFormulaTokenVo> tokens, String kind, String read,
                                            String filter, String filterRef, BigDecimal constant,
                                            SkillValueReader reader) {
        if (tokens != null && !tokens.isEmpty()) {
            return evalTokens(tokens, reader);
        }
        return resolveOperand(kind, read, filter, filterRef, constant, reader);
    }

    public BigDecimal evalFormula(SkillFormulaGroupVo formula, SkillValueReader reader) {
        if (formula == null || formula.getTokens() == null || formula.getTokens().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return evalTokens(formula.getTokens(), reader);
    }

    public BigDecimal evalTokens(List<SkillFormulaTokenVo> tokens, SkillValueReader reader) {
        List<String> rpn = toRpn(tokens, reader);
        Deque<BigDecimal> stack = new ArrayDeque<>();
        for (String t : rpn) {
            if ("+".equals(t) || "-".equals(t) || "*".equals(t) || "/".equals(t)) {
                BigDecimal b = stack.isEmpty() ? BigDecimal.ZERO : stack.pop();
                BigDecimal a = stack.isEmpty() ? BigDecimal.ZERO : stack.pop();
                stack.push(switch (t) {
                    case "+" -> a.add(b);
                    case "-" -> a.subtract(b);
                    case "*" -> a.multiply(b);
                    case "/" -> b.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : a.divide(b, 6, RoundingMode.HALF_UP);
                    default -> BigDecimal.ZERO;
                });
            } else {
                stack.push(new BigDecimal(t));
            }
        }
        return stack.isEmpty() ? BigDecimal.ZERO : stack.pop();
    }

    public SkillValueReader unitReader(BattleUnit unit, Map<SkillReadType, BigDecimal> eventValues) {
        return (type, filter, filterRef) -> {
            if (type == null) {
                return null;
            }
            if (eventValues != null && eventValues.containsKey(type)) {
                return eventValues.get(type);
            }
            if (unit == null) {
                return null;
            }
            return switch (type) {
                case CHAR_ATTACK -> BigDecimal.valueOf(unit.getAttack() != null ? unit.getAttack() : 0);
                case CHAR_MAX_HP -> BigDecimal.valueOf(unit.getMaxHp() != null ? unit.getMaxHp() : 0);
                case CHAR_CUR_HP -> BigDecimal.valueOf(unit.getHp() != null ? unit.getHp() : 0);
                case CHAR_DEFENSE -> BigDecimal.valueOf(unit.getDefense() != null ? unit.getDefense() : 0);
                case CHAR_CUR_ACTION -> BigDecimal.valueOf(unit.getActionBar() != null ? unit.getActionBar() : 0);
                case CHAR_MAX_ACTION -> BigDecimal.valueOf(unit.getActionValue() != null ? unit.getActionValue() : 0);
                case WEAPON_ATTACK -> BigDecimal.valueOf(unit.getWeaponAttack() != null ? unit.getWeaponAttack() : 0);
                case WEAPON_DAMAGE_RATIO -> unit.getWeaponDamageRatio() != null
                        ? unit.getWeaponDamageRatio()
                        : BigDecimal.ONE;
                case EQUIP_USES_LEFT -> BigDecimal.valueOf(9999);
                default -> null;
            };
        };
    }

    private BigDecimal resolveOperand(String kind, String read, String filter, String filterRef, BigDecimal constant,
                                      SkillValueReader reader) {
        SkillOperandKind k = SkillOperandKind.parse(kind);
        if (k == SkillOperandKind.CONST) {
            return constant;
        }
        SkillReadType rt = SkillReadType.parse(read);
        if (rt == null || reader == null) {
            return null;
        }
        return reader.read(rt, filter, filterRef);
    }

    private List<String> toRpn(List<SkillFormulaTokenVo> tokens, SkillValueReader reader) {
        List<String> output = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();
        for (SkillFormulaTokenVo token : tokens) {
            if (token == null || token.getKind() == null) {
                continue;
            }
            String kind = token.getKind().toUpperCase();
            switch (kind) {
                case "CONST" -> output.add(token.getValue() != null ? token.getValue().toPlainString() : "0");
                case "READ" -> {
                    BigDecimal v = resolveOperand("READ", token.getRead(), token.getFilter(), token.getFilterRef(),
                            null, reader);
                    output.add(v != null ? v.toPlainString() : "0");
                }
                case "LPAREN" -> ops.push("(");
                case "RPAREN" -> {
                    while (!ops.isEmpty() && !"(".equals(ops.peek())) {
                        output.add(ops.pop());
                    }
                    if (!ops.isEmpty()) {
                        ops.pop();
                    }
                }
                case "OP" -> {
                    String op = token.getOp();
                    while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(op)) {
                        output.add(ops.pop());
                    }
                    ops.push(op);
                }
                default -> {
                }
            }
        }
        while (!ops.isEmpty()) {
            String op = ops.pop();
            if (!"(".equals(op) && !")".equals(op)) {
                output.add(op);
            }
        }
        return output;
    }

    private int precedence(String op) {
        if ("*".equals(op) || "/".equals(op)) {
            return 2;
        }
        if ("+".equals(op) || "-".equals(op)) {
            return 1;
        }
        return 0;
    }
}
