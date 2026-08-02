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
@TableName("app_game_item_trigger")
public class GameItemTrigger extends WxBaseEntity<GameItemTrigger> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String itemId;

    private String skillId;

    private BigDecimal thresholdValue;

    private String triggerPhase;

    /** @deprecated 已由 skill 组合替代，仅兼容旧数据 */
    private String effectType;

    /** @deprecated 已由 skill 组合替代，仅兼容旧数据 */
    private BigDecimal effectValue;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
