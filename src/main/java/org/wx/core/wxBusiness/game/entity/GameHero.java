package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBase.unit.WordUnit;

/**
 * PVE 主角
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_hero")
public class GameHero extends WxBaseEntity<GameHero> {

    public static final int DEFAULT_MAX_HP = 200;
    public static final int DEFAULT_ATTACK = 10;
    public static final int DEFAULT_ACTION_VALUE = 100;
    public static final String DEFAULT_NAME = "主角";

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String uid;

    private String name;

    private Integer hp;

    private Integer maxHp;

    private Integer attack;

    private Integer actionValue;

    public static GameHero defaultHero(String uid) {
        GameHero hero = new GameHero();
        hero.setId(WordUnit.randomKey(10, 1));
        hero.setUid(uid);
        hero.setName(DEFAULT_NAME);
        hero.setMaxHp(DEFAULT_MAX_HP);
        hero.setHp(DEFAULT_MAX_HP);
        hero.setAttack(DEFAULT_ATTACK);
        hero.setActionValue(DEFAULT_ACTION_VALUE);
        return hero;
    }
}
