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
@TableName("app_game_profession_skill")
public class GameProfessionSkill extends WxBaseEntity<GameProfessionSkill> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String professionId;

    private String itemId;

    private Integer sort;

    private Integer enabled;

    @TableField(exist = false)
    private String itemName;

    @TableField(exist = false)
    private String itemCode;
}
