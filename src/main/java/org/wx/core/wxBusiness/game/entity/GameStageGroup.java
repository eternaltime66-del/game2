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
@TableName("app_game_stage_group")
public class GameStageGroup extends WxBaseEntity<GameStageGroup> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String chapterId;

    private Integer groupNo;

    private String name;

    private Integer sort;

    private Integer enabled;

    private String remark;

    @TableField(exist = false)
    private String chapterName;
}
