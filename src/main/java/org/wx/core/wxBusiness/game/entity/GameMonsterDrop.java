package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_monster_drop")
public class GameMonsterDrop extends WxBaseEntity<GameMonsterDrop> {

    @TableId(type = IdType.INPUT)
    private String id;

    private String monsterId;

    private String itemId;

    /** 掉落概率 0-100 */
    private Integer dropRate;

    private Integer minQty;

    private Integer maxQty;

    private Integer sort;

    private Integer enabled;

    private String remark;
}
