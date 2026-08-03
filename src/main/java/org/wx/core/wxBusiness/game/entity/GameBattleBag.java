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
@TableName("app_game_battle_bag")
public class GameBattleBag extends WxBaseEntity<GameBattleBag> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String uid;

    private String itemId;

    private Integer quantity;

    private Integer sort;
}
