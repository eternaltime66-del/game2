package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBase.unit.WordUnit;

import java.math.BigDecimal;

/**
 * PVE 主角
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_hero")
public class GameHero extends WxBaseEntity<GameHero> {

    public static final int DEFAULT_MAX_HP = 200;
    public static final int DEFAULT_ATTACK = 10;
    public static final int DEFAULT_DEFENSE = 0;
    public static final int DEFAULT_ACTION_VALUE = 100;
    public static final String DEFAULT_NAME = "主角";
    public static final BigDecimal DEFAULT_OPTIMAL_CARRY_WEIGHT = BigDecimal.valueOf(10);

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String uid;

    private String name;

    private Integer hp;

    private Integer maxHp;

    private Integer attack;

    private Integer defense;

    private Integer actionValue;

    /** 布阵列 0-3（主角占地宽 2，默认 1 = B 列起） */
    private Integer slotCol;

    /** 布阵行 0-2（0=前排，默认 0） */
    private Integer slotRow;

    private BigDecimal optimalCarryWeight;

    public static GameHero defaultHero(String uid) {
        GameHero hero = new GameHero();
        hero.setId(WordUnit.randomKey(10, 1));
        hero.setUid(uid);
        hero.setName(DEFAULT_NAME);
        hero.setMaxHp(DEFAULT_MAX_HP);
        hero.setHp(DEFAULT_MAX_HP);
        hero.setAttack(DEFAULT_ATTACK);
        hero.setDefense(DEFAULT_DEFENSE);
        hero.setActionValue(DEFAULT_ACTION_VALUE);
        hero.setSlotCol(1);
        hero.setSlotRow(0);
        hero.setOptimalCarryWeight(DEFAULT_OPTIMAL_CARRY_WEIGHT);
        return hero;
    }
}
