package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

@Data
public class BattleLog {

    public static final String TYPE_TICK = "TICK";
    public static final String TYPE_ACTION = "ACTION";
    public static final String TYPE_WAVE = "WAVE";
    public static final String TYPE_RESULT = "RESULT";

    private String type;

    private String text;

    public static BattleLog of(String type, String text) {
        BattleLog log = new BattleLog();
        log.setType(type);
        log.setText(text);
        return log;
    }
}
