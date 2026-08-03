package org.wx.core.wxBusiness.game.entity;

import lombok.Data;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminFinishedSkillVo {

    private String id;

    private String code;

    private String name;

    private String targetType;

    private String targetTypeLabel;

    private Integer targetParam;

    /** 频率槽，最小 1 */
    private Integer hitFrequency;

    /** null=无限 */
    private Integer maxCastCount;

    private Boolean maxCastUnlimited;

    private List<SkillFormulaGroupVo> formulas = new ArrayList<>();

    private String catL1;

    private String catL1Label;

    private String catL2;

    private String catL2Label;

    private String catL3;

    /** 分类3 为入口名称，与 catL3 同值 */
    private String catL3Label;

    private String catL4;

    private String catL4Label;

    private Integer enabled;

    private String remark;

    /** @deprecated 旧效果步骤 */
    private List<AdminFinishedSkillEffectVo> effects = new ArrayList<>();

    /** 是否只读（通用普攻） */
    private Boolean readonly;
}
