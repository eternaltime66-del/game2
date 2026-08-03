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

    /** PRECISE / QUICK */
    private String triggerMode;

    /** 快捷扳机预设 */
    private String quickPreset;

    /** 条件组 JSON（精准扳机） */
    private String conditionsJson;

    /** @deprecated 旧扳机类型，清库后不再使用 */
    private String triggerSlotType;

    /** @deprecated */
    private BigDecimal triggerParam;

    /** @deprecated */
    private String triggerRefId;

    private String finishedSkillId;

    /** @deprecated 上限改到成品技能上；槽位字段保留兼容 */
    private Integer maxCastCount;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
