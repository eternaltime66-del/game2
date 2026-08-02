package org.wx.core.wxBusiness.game.entity;

import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;

import java.util.ArrayList;
import java.util.List;

public final class ItemTagHelper {

    private ItemTagHelper() {
    }

    public static void fillTags(ItemTagHolder holder, GameItem item) {
        if (holder == null || item == null) {
            return;
        }
        List<String> labels = new ArrayList<>(GameItemTag.toLabels(item.getItemTags()));
        List<String> codes = new ArrayList<>(GameItemTag.toCodes(item.getItemTags()));
        holder.setTags(labels);
        setTagCodes(holder, codes);
    }

    private static void setTagCodes(ItemTagHolder holder, List<String> codes) {
        if (holder instanceof InventorySlotVo slotVo) {
            slotVo.setItemTagCodes(codes);
        } else if (holder instanceof ItemDetailVo detailVo) {
            detailVo.setItemTagCodes(codes);
        }
    }

    public static boolean hasTag(GameItem item, GameItemTag tag) {
        return item != null && GameItemTag.contains(item.getItemTags(), tag);
    }
}
