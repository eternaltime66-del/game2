package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MonsterDetailVo {

    private String id;

    private String code;

    private String name;

    private Integer hp;

    private Integer maxHp;

    private Integer attack;

    private Integer actionValue;

    private String remark;

    private List<MonsterDropPreviewVo> drops = new ArrayList<>();
}
