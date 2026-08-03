package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_armor")
public class GameArmor extends WxBaseEntity<GameArmor> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String itemId;

    private Integer bonusHp;

    private Integer defense;

    private Integer bonusAttack;

    private Integer enabled;

    private String remark;
}
