package org.wx.core.wxBusiness.game.entity;

import lombok.Data;
import org.wx.core.wxBusiness.game.entity.skill.PassiveConditionVo;
import org.wx.core.wxBusiness.game.entity.skill.PassiveEffectVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminPassiveSkillVo {

    private String id;

    private String code;

    private String name;

    private String passiveKind;

    private String passiveKindLabel;

    private String catL1;

    private String catL2;

    private String catL2Label;

    private String ownerRef;

    private List<PassiveConditionVo> conditions = new ArrayList<>();

    private List<PassiveEffectVo> effects = new ArrayList<>();

    /** @deprecated */
    private String conditionType;

    private String conditionTypeLabel;

    private String conditionEquipItemId;

    private String conditionEquipItemName;

    private String effectType;

    private String effectTypeLabel;

    private BigDecimal effectValue;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
