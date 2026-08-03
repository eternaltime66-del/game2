package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_weapon")
public class GameWeapon extends WxBaseEntity<GameWeapon> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String itemId;

    private Integer attack;

    private Integer baseActionValue;

    private BigDecimal damageRatio;

    private Integer enabled;

    private String remark;
}
