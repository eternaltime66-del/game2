package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_monster_passive")
public class GameMonsterPassive extends WxBaseEntity<GameMonsterPassive> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String monsterId;

    private String passiveSkillId;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
