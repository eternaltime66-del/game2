package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameSkillTargetType;
import org.wx.core.wxBusiness.game.entity.enums.GameTriggerEffectType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 完整技能执行：按步骤组合基础效果
 */
@Service
public class GameSkillExecutorService {

    @Resource
    private GameSkillService skillService;
    @Resource
    private GameSkillEffectService skillEffectService;
    @Resource
    @Lazy
    private CombatTriggerService combatTriggerService;

    public List<BattleLog> executeSkill(String skillId, CombatSkillContext ctx) {
        if (skillId == null || skillId.isBlank() || ctx == null) {
            return Collections.emptyList();
        }
        GameSkill skill = skillService.getById(skillId);
        if (skill == null || !Integer.valueOf(1).equals(skill.getEnabled())) {
            return Collections.emptyList();
        }

        List<GameSkillEffect> effects = skillEffectService.listBySkillId(skillId);
        if (effects.isEmpty()) {
            return Collections.emptyList();
        }

        List<BattleLog> logs = new ArrayList<>();
        String source = ctx.getSourceName() != null ? ctx.getSourceName() : skill.getName();
        logs.add(BattleLog.trigger("【" + source + "】释放技能「" + skill.getName() + "」"));

        for (GameSkillEffect effect : effects) {
            GameTriggerEffectType effectType = GameTriggerEffectType.parse(effect.getEffectType());
            if (effectType == null) {
                continue;
            }
            BigDecimal value = combatTriggerService.normalizeValue(effect.getEffectValue());
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            GameSkillTargetType targetType = GameSkillTargetType.parse(effect.getTargetType());
            if (targetType == null) {
                targetType = GameSkillTargetType.SELF;
            }
            BattleUnit effectTarget = resolveTarget(targetType, ctx);
            if (effectTarget == null || !effectTarget.isAlive()) {
                continue;
            }

            logs.addAll(combatTriggerService.applySkillEffect(ctx, effectType, value, effectTarget));
            logs.add(BattleLog.trigger(buildStepText(skill.getName(), effectType, effectTarget, value)));

            if (ctx.getActor() != null && !ctx.getActor().isAlive()) {
                break;
            }
            if (ctx.getAttackTarget() != null && !ctx.getAttackTarget().isAlive()) {
                break;
            }
            if (ctx.getOwner() != null && !ctx.getOwner().isAlive()) {
                break;
            }
        }
        return logs;
    }

    private BattleUnit resolveTarget(GameSkillTargetType targetType, CombatSkillContext ctx) {
        return switch (targetType) {
            case SELF -> ctx.getOwner();
            case ATTACK_TARGET -> ctx.getAttackTarget();
            case ATTACKER -> ctx.getActor();
        };
    }

    private String buildStepText(String skillName,
                                 GameTriggerEffectType effectType,
                                 BattleUnit target,
                                 BigDecimal value) {
        String amount = CombatDamageService.formatDamage(value);
        return switch (effectType) {
            case DEAL_DAMAGE -> "「" + skillName + "」对 " + target.getName() + " 造成 " + amount + " 伤害";
            case TAKE_DAMAGE -> "「" + skillName + "」" + target.getName() + " 受到 " + amount + " 伤害";
            case HEAL -> "「" + skillName + "」" + target.getName() + " 恢复 " + amount + " 生命";
        };
    }
}
