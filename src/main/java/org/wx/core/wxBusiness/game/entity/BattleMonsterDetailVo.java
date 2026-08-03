package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BattleMonsterDetailVo {

    private String monsterId;

    private String code;

    private String name;

    private Integer hp;

    private Integer maxHp;

    private Integer attack;

    private Integer actionValue;

    /** NORMAL / ELITE / BOSS */
    private String rankType;

    private String rankLabel;

    private Integer footprintW;

    private Integer footprintH;

    private String remark;

    private List<BattleMonsterDropItemVo> drops = new ArrayList<>();
}
