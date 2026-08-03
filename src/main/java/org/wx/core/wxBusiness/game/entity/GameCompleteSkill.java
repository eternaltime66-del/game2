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
@TableName("app_game_complete_skill")
public class GameCompleteSkill extends WxBaseEntity<GameCompleteSkill> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String code;

    private String name;

    private String triggerSlotType;

    private BigDecimal triggerParam;

    private String triggerRefId;

    private String finishedSkillId;

    private String bindType;

    private String bindRefId;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
