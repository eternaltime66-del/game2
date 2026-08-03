package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_recipe_material")
public class GameRecipeMaterial extends WxBaseEntity<GameRecipeMaterial> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String recipeId;

    private String materialItemId;

    private Integer quantity;

    private Integer sort;
}
