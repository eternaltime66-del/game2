package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItemFinishedSkillDetailVo {

    private String id;

    private String code;

    private String name;

    private String targetType;

    private String targetLabel;

    private Integer targetParam;

    /** 分类展示，与后台一致，如 装备 · 武器 · 铁剑 · 普攻 */
    private String categoryLabel;

    private String remark;

    /** 频率槽 */
    private Integer hitFrequency;

    /** 全场发动上限 */
    private Integer maxCastCount;

    private String castLimitText;

    private List<ItemSkillEffectDetailVo> effects = new ArrayList<>();
}
