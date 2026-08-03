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
@TableName("app_game_trigger_slot")
public class GameTriggerSlot extends WxBaseEntity<GameTriggerSlot> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String itemId;

    private String monsterId;

    /** BASIC_ATTACK=武器普攻 ULTIMATE=武器大招 TRAIT_ACTIVE=特性主动 */
    private String slotKind;

    private String triggerSlotType;

    private BigDecimal triggerParam;

    private String triggerRefId;

    private String finishedSkillId;

    /** 单场最多释放次数，null 表示无限 */
    private Integer maxCastCount;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
