package org.wx.core.wxBusiness.game.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 战斗单位运行时
 */
@Data
public class BattleUnit {

    public static final String SIDE_HERO = "HERO";
    public static final String SIDE_MONSTER = "MONSTER";

    private String unitId;

    private String side;

    private String name;

    private BigDecimal hp;

    private Integer maxHp;

    private Integer attack;

    /** 防御（受击减免用） */
    private Integer defense;

    /** 行动条上限 */
    private Integer actionValue;

    /** 当前行动条（整数，无小数） */
    private Integer actionBar;

    private String monsterId;

    private Boolean alive;

    public void initActionBar() {
        this.actionBar = 0;
    }

    @JSONField(serialize = false, deserialize = false)
    public boolean isAlive() {
        return alive != null && alive && hp != null && hp.compareTo(BigDecimal.ZERO) > 0;
    }
}
