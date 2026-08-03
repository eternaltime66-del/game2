package org.wx.core.wxBusiness.game.entity.skill;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 被动生效条件一条 */
@Data
public class PassiveConditionVo {
    /** NONE / REQUIRE_EQUIP / COMPARE */
    private String type;
    private String equipItemId;
    /** COMPARE 时复用条件项字段 */
    private String leftKind;
    private String leftRead;
    private String leftFilter;
    private String leftFilterRef;
    private BigDecimal leftConst;
    private String op;
    private String rightKind;
    private String rightRead;
    private String rightFilter;
    private String rightFilterRef;
    private BigDecimal rightConst;
}
