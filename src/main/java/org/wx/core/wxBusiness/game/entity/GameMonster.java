package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_monster")
public class GameMonster extends WxBaseEntity<GameMonster> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String code;

    private String name;

    private Integer hp;

    private Integer maxHp;

    private Integer attack;

    private Integer actionValue;

    /** NORMAL / ELITE / BOSS */
    private String rankType;

    /** 占地宽（列数） */
    private Integer footprintW;

    /** 占地高（行数） */
    private Integer footprintH;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
