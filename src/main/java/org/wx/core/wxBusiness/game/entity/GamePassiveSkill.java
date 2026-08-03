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
@TableName("app_game_passive_skill")
public class GamePassiveSkill extends WxBaseEntity<GamePassiveSkill> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String code;

    private String name;

    /** NUMERIC / MECHANISM */
    private String passiveKind;

    /** EQUIP / PERSON */
    private String catL1;

    /** 装备部位 或 CHARACTER/PROFESSION/GENERAL */
    private String catL2;

    /** 角色模板 code 或职业 id */
    private String ownerRef;

    private String conditionType;

    private String conditionEquipItemId;

    private String effectType;

    private BigDecimal effectValue;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
