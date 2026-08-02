package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_chapter")
public class GameChapter extends WxBaseEntity<GameChapter> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String groupId;

    private String code;

    private String name;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
