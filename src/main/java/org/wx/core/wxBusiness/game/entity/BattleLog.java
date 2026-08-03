package org.wx.core.wxBusiness.game.entity;

import lombok.Data;
import org.wx.core.wxBusiness.game.entity.enums.FinishedSkillCatL4;

@Data
public class BattleLog {

    public static final String TYPE_TICK = "TICK";
    public static final String TYPE_ACTION = "ACTION";
    public static final String TYPE_WAVE = "WAVE";
    public static final String TYPE_RESULT = "RESULT";
    public static final String TYPE_SKILL = "SKILL";
    public static final String TYPE_LOOT = "LOOT";

    private String type;

    private String text;

    private String actorName;

    private String targetName;

    private String damage;

    /** 伤害计算公式说明，如 (攻击力 105 × 100% × 武器 150%) */
    private String damageFormula;

    private Boolean killed;

    /** 技能名（SKILL 类型结构化字段） */
    private String skillName;

    /** 写入日志时的全场总轴 */
    private Integer axis;

    public static BattleLog of(String type, String text) {
        BattleLog log = new BattleLog();
        log.setType(type);
        log.setText(text);
        return log;
    }

    public static BattleLog action(String actorName, String targetName, String damage, boolean killed) {
        return action(actorName, targetName, damage, null, killed);
    }

    public static BattleLog action(String actorName, String targetName, String damage, String damageFormula, boolean killed) {
        BattleLog log = new BattleLog();
        log.setType(TYPE_ACTION);
        log.setActorName(actorName);
        log.setTargetName(targetName);
        log.setDamage(damage);
        log.setDamageFormula(damageFormula);
        String text = actorName + " 攻击 " + targetName + " 造成 " + damage + " 伤害";
        if (damageFormula != null && !damageFormula.isBlank()) {
            text += " " + damageFormula;
        }
        if (killed) {
            text += "，" + targetName + " 死亡";
        }
        log.setText(text);
        return log;
    }

    public static BattleLog skillDamage(String actorName, String targetName, String skillName,
                                        String damage, boolean killed) {
        return skillDamage(actorName, targetName, skillName, damage, null, killed);
    }

    public static BattleLog skillDamage(String actorName, String targetName, String skillName,
                                        String damage, String damageFormula, boolean killed) {
        BattleLog log = new BattleLog();
        log.setType(TYPE_SKILL);
        log.setActorName(actorName);
        log.setTargetName(targetName);
        log.setSkillName(skillName);
        log.setDamage(damage);
        log.setDamageFormula(damageFormula);
        log.setKilled(killed);
        String text = actorName + " 发动「" + skillName + "」对 " + targetName + " 造成 " + damage + " 伤害";
        if (damageFormula != null && !damageFormula.isBlank()) {
            text += " " + damageFormula;
        }
        if (killed) {
            text += "，" + targetName + " 死亡";
        }
        log.setText(text);
        return log;
    }

    public static BattleLog skillHeal(String actorName, String targetName, String skillName, String heal) {
        BattleLog log = new BattleLog();
        log.setType(TYPE_SKILL);
        log.setActorName(actorName);
        log.setTargetName(targetName);
        log.setSkillName(skillName);
        log.setText(actorName + " 发动「" + skillName + "」为 " + targetName + " 恢复 " + heal + " 生命");
        return log;
    }

    public static BattleLog loot(String text) {
        BattleLog log = new BattleLog();
        log.setType(TYPE_LOOT);
        log.setText(text);
        return log;
    }

    /** 战斗日志用短标签，如「铁剑 - 普攻」 */
    public static String buildSkillDisplayLabel(GameFinishedSkill skill) {
        if (skill == null) {
            return "技能";
        }
        FinishedSkillCatL4 catL4 = FinishedSkillCatL4.parse(skill.getCatL4());
        String slotLabel = catL4.getLabel();
        String entry = skill.getCatL3();
        if (entry != null && !entry.isBlank() && !"通用".equals(entry.trim())) {
            return entry.trim() + " - " + slotLabel;
        }
        return shortenSkillName(skill.getName(), slotLabel);
    }

    private static String shortenSkillName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String trimmed = name.trim();
        if (!trimmed.contains(" - ") && !trimmed.contains("·")) {
            return trimmed;
        }
        String[] parts = trimmed.split("\\s*[-·]\\s*");
        if (parts.length >= 2) {
            return parts[parts.length - 2].trim() + " - " + parts[parts.length - 1].trim();
        }
        return trimmed;
    }
}
