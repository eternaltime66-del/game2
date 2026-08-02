package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_item_log")
public class GameItemLog extends WxBaseEntity<GameItemLog> {

    public static final String REASON_BATTLE_WIN = "BATTLE_WIN";
    public static final String REASON_DISCARD = "DISCARD";
    public static final String REASON_TO_BAG = "TO_BAG";
    public static final String REASON_TO_WAREHOUSE = "TO_WAREHOUSE";
    public static final String REASON_EQUIP = "EQUIP";
    public static final String REASON_UNEQUIP = "UNEQUIP";
    public static final String REASON_CRAFT = "CRAFT";
    public static final String REASON_CRAFT_COST = "CRAFT_COST";

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String uid;

    private String itemId;

    private String itemName;

    private Integer changeQty;

    private Integer beforeQty;

    private Integer afterQty;

    private String reason;

    private String refId;

    private String remark;

    public static GameItemLog of(
            String uid,
            String itemId,
            String itemName,
            int changeQty,
            int beforeQty,
            int afterQty,
            String reason,
            String refId,
            String remark
    ) {
        GameItemLog log = new GameItemLog();
        log.setUid(uid);
        log.setItemId(itemId);
        log.setItemName(itemName);
        log.setChangeQty(changeQty);
        log.setBeforeQty(beforeQty);
        log.setAfterQty(afterQty);
        log.setReason(reason);
        log.setRefId(refId);
        log.setRemark(remark);
        return log;
    }
}
