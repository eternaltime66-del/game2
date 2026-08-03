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
@TableName("app_game_item")
public class GameItem extends WxBaseEntity<GameItem> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String code;

    private String name;

    private String icon;

    private BigDecimal weight;

    private String itemTags;

    private Integer maxStack;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
