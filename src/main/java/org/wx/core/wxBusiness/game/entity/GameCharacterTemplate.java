package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_character_template")
public class GameCharacterTemplate extends WxBaseEntity<GameCharacterTemplate> {

    public static final String CODE_PROTAGONIST = "PROTAGONIST";

    @TableId(type = IdType.INPUT)
    private String id;

    private String code;

    private String name;

    private Integer maxHp;

    private Integer attack;

    private Integer defense;

    private Integer actionValue;

    private Integer templateVersion;

    private Integer enabled;

    private String remark;

    /** 绑定职业 ID 列表（多选，非表字段） */
    @TableField(exist = false)
    private List<String> professionIds = new ArrayList<>();

    /** 绑定职业名称摘要（列表展示） */
    @TableField(exist = false)
    private String professionNames;
}
