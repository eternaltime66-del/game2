package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_finished_skill")
public class GameFinishedSkill extends WxBaseEntity<GameFinishedSkill> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String code;

    private String name;

    private String targetType;

    private Integer targetParam;

    /** 频率槽：对目标触发几下，最小 1 */
    private Integer hitFrequency;

    /** 全场最多发动次数，null=无限 */
    private Integer maxCastCount;

    /** 公式组 JSON */
    private String formulasJson;

    /** 分类1：通用/装备/怪物 */
    private String catL1;

    /** 分类2：武器/护甲等 */
    private String catL2;

    /** 分类3：入口名称（如 大剑、某怪物名） */
    private String catL3;

    /** 分类4：普攻/大招/特性主动等 */
    private String catL4;

    private Integer enabled;

    private String remark;
}
