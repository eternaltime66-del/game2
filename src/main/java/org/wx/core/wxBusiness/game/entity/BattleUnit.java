package org.wx.core.wxBusiness.game.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

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

    private Integer hp;

    private Integer maxHp;

    private Integer attack;

    private Integer defense;

    /** 行动条上限 */
    private Integer actionValue;

    /** 当前行动条（整数，无小数） */
    private Integer actionBar;

    /** 装备武器伤害比例（战斗初始化时写入，供技能公式 z 使用） */
    private java.math.BigDecimal weaponDamageRatio;

    private String monsterId;

    private Boolean alive;

    public void initActionBar() {
        this.actionBar = 0;
    }

    @JSONField(serialize = false, deserialize = false)
    public boolean isAlive() {
        return alive != null && alive && hp != null && hp > 0;
    }
}
