package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

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
}
