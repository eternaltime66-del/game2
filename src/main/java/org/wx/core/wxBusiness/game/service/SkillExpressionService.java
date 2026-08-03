package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Component;
import org.wx.core.wxBusiness.game.entity.BattleUnit;
import org.wx.core.wxBusiness.game.entity.enums.SkillCompareOp;
import org.wx.core.wxBusiness.game.entity.enums.SkillOperandKind;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadType;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionItemVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaTokenVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 条件判定 + 公式求值 */
@Component
public class SkillExpressionService {

    public boolean anyGroupMatch(List<SkillConditionGroupVo> groups, Function<SkillReadType, BigDecimal> reader) {
        if (groups == null || groups.isEmpty()) {
            return true;
        }
        for (SkillConditionGroupVo group : groups) {
            if (groupMatch(group, reader)) {
                return true;
            }
        }
        return false;
    }

    public boolean groupMatch(SkillConditionGroupVo group, Function<SkillReadType, BigDecimal> reader) {
        if (group == null || group.getItems() == null || group.getItems().isEmpty()) {
            return true;
        }
        for (SkillConditionItemVo item : group.getItems()) {
            if (!itemMatch(item, reader)) {
                return false;
            }
        }
        return true;
    }

    public boolean itemMatch(SkillConditionItemVo item, Function<SkillReadType, BigDecimal> reader) {
        if (item == null || item.getOp() == null) {
            return true;
        }
        BigDecimal left = resolveOperand(item.getLeftKind(), item.getLeftRead(), item.getLeftConst(), reader);
        BigDecimal right = resolveOperand(item.getRightKind(), item.getRightRead(), item.getRightConst(), reader);
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
        };
    }

    public BigDecimal evalFormula(SkillFormulaGroupVo formula, Function<SkillReadType, BigDecimal> reader) {
        if (formula == null || formula.getTokens() == null || formula.getTokens().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return evalTokens(formula.getTokens(), reader);
    }

    public BigDecimal evalTokens(List<SkillFormulaTokenVo> tokens, Function<SkillReadType, BigDecimal> reader) {
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

    public Function<SkillReadType, BigDecimal> unitReader(BattleUnit unit, Map<SkillReadType, BigDecimal> eventValues) {
        return type -> {
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
                case CHAR_DEFENSE -> BigDecimal.valueOf(unit.getDefense() != null ? unit.getDefense() : 0);
                case CHAR_CUR_ACTION -> BigDecimal.valueOf(unit.getActionBar() != null ? unit.getActionBar() : 0);
                case CHAR_MAX_ACTION -> BigDecimal.valueOf(unit.getActionValue() != null ? unit.getActionValue() : 0);
                case WEAPON_DAMAGE_RATIO -> unit.getWeaponDamageRatio() != null
                        ? unit.getWeaponDamageRatio() : BigDecimal.ONE;
                case EQUIP_USES_LEFT -> BigDecimal.valueOf(9999);
                default -> null;
            };
        };
    }

    private BigDecimal resolveOperand(String kind, String read, BigDecimal constant,
                                      Function<SkillReadType, BigDecimal> reader) {
        SkillOperandKind k = SkillOperandKind.parse(kind);
        if (k == SkillOperandKind.CONST) {
            return constant;
        }
        SkillReadType rt = SkillReadType.parse(read);
        if (rt == null) {
            return null;
        }
        return reader.apply(rt);
    }

    private List<String> toRpn(List<SkillFormulaTokenVo> tokens, Function<SkillReadType, BigDecimal> reader) {
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
                    BigDecimal v = resolveOperand("READ", token.getRead(), null, reader);
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
