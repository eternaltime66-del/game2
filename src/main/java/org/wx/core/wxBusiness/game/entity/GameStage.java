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
@TableName("app_game_stage")
public class GameStage extends WxBaseEntity<GameStage> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String stageGroupId;

    private Integer stageNo;

    private String name;

    private Integer sort;

    private Integer enabled;

    private String remark;

    @TableField(exist = false)
    private Integer groupNo;

    @TableField(exist = false)
    private String displayCode;

    public void fillDisplayCode(Integer groupNoValue) {
        this.groupNo = groupNoValue;
        if (groupNoValue != null && stageNo != null) {
            this.displayCode = groupNoValue + "-" + stageNo;
        }
    }
}
