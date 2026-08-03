package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_recipe")
public class GameRecipe extends WxBaseEntity<GameRecipe> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String outputItemId;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
