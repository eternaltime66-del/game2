package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_inventory")
public class GameInventory extends WxBaseEntity<GameInventory> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String uid;

    private Integer slotNo;

    private String itemId;

    private Integer quantity;
}
