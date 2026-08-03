package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_stage_drop")
public class GameStageDrop extends WxBaseEntity<GameStageDrop> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String stageId;

    private String itemId;

    private Integer enabled;

    private String remark;
}
