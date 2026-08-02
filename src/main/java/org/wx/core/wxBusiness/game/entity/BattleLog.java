package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class BattleLog {

    public static final String TYPE_TICK = "TICK";
    public static final String TYPE_ACTION = "ACTION";
    public static final String TYPE_WAVE = "WAVE";
    public static final String TYPE_RESULT = "RESULT";
    public static final String TYPE_LOOT = "LOOT";
    public static final String TYPE_TRIGGER = "TRIGGER";

    private String type;

    private String text;

    private String actorName;

    private String targetName;

    private String damage;

    private Boolean killed;

    public static BattleLog of(String type, String text) {
        BattleLog log = new BattleLog();
        log.setType(type);
        log.setText(text);
        return log;
    }

    public static BattleLog action(String actorName, String targetName, String damage, boolean killed) {
        BattleLog log = new BattleLog();
        log.setType(TYPE_ACTION);
        log.setActorName(actorName);
        log.setTargetName(targetName);
        log.setDamage(damage);
        log.setKilled(killed);
        String text = actorName + " 攻击 " + targetName + " 造成 " + damage + " 伤害";
        if (killed) {
            text += "，" + targetName + " 死亡";
        }
        log.setText(text);
        return log;
    }

    public static BattleLog loot(String text) {
        BattleLog log = new BattleLog();
        log.setType(TYPE_LOOT);
        log.setText(text);
        return log;
    }

    public static BattleLog trigger(String text) {
        BattleLog log = new BattleLog();
        log.setType(TYPE_TRIGGER);
        log.setText(text);
        return log;
    }
}
