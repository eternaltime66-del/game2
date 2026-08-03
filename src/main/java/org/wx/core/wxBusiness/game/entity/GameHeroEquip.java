package org.wx.core.wxBusiness.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.entity.enums.HeroEquipSlot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_game_hero_equip")
public class GameHeroEquip extends WxBaseEntity<GameHeroEquip> {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String uid;

    private String weaponItemId;

    /** 消耗型武器剩余次数 */
    private Integer weaponUsesLeft;

    private String armorItemId;

    private String glovesItemId;

    private String legsItemId;

    private String helmetItemId;

    private String accessory1ItemId;

    private String accessory2ItemId;

    private String accessory3ItemId;

    private String skillBadge1ItemId;

    private String skillBadge2ItemId;

    private String skillBadge3ItemId;

    private String skillBadge4ItemId;

    public static GameHeroEquip empty(String uid) {
        GameHeroEquip row = new GameHeroEquip();
        row.setId(WordUnit.randomKey(10, 1));
        row.setUid(uid);
        return row;
    }

    public List<String> listEquippedItemIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (HeroEquipSlot slot : HeroEquipSlot.values()) {
            String itemId = slot.getItemId(this);
            if (itemId != null && !itemId.isBlank()) {
                ids.add(itemId);
            }
        }
        return new ArrayList<>(ids);
    }
}
