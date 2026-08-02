package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_wave_monster")
public class GameWaveMonster extends WxBaseEntity<GameWaveMonster> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String waveId;

    private String monsterId;

    private Integer quantity;

    private Integer sort;

    @TableField(exist = false)
    private String monsterName;

    @TableField(exist = false)
    private String monsterCode;

    @TableField(exist = false)
    private Integer hp;

    @TableField(exist = false)
    private Integer maxHp;

    @TableField(exist = false)
    private Integer attack;

    @TableField(exist = false)
    private Integer actionValue;
}
