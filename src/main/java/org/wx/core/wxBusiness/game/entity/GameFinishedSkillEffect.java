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
@TableName("app_game_finished_skill_effect")
public class GameFinishedSkillEffect extends WxBaseEntity<GameFinishedSkillEffect> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String finishedSkillId;

    private String effectKind;

    private String outcomeType;

    private String statRef;

    private BigDecimal ratioY;

    /** 1=由武器释放，z 运行时读取装备武器 damage_ratio */
    private Integer useWeaponRatio;

    /** @deprecated 已改为 useWeaponRatio + 运行时武器比例 */
    private BigDecimal ratioZ;

    private BigDecimal fixedValue;

    private Integer actionDelta;

    private Integer sort;

    private String remark;
}
