package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_skill_badge")
public class GameSkillBadge extends WxBaseEntity<GameSkillBadge> {

    @TableId(type = IdType.INPUT)
    private String itemId;

    private String passiveSkillId;
}
