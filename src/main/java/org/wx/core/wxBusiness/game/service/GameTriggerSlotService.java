package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameTriggerSlot;
import org.wx.core.wxBusiness.game.mapper.GameTriggerSlotMapper;

import java.util.Collections;
import java.util.List;

@Service
public class GameTriggerSlotService extends WxServiceImpl<GameTriggerSlotMapper, GameTriggerSlot> {

    public List<GameTriggerSlot> listByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Collections.emptyList();
        }
        return this.find()
                .eq(GameTriggerSlot::getItemId, itemId)
                .orderByAsc(GameTriggerSlot::getSort)
                .list();
    }

    public List<GameTriggerSlot> listEnabledByItemIds(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.find()
                .in(GameTriggerSlot::getItemId, itemIds)
                .eq(GameTriggerSlot::getEnabled, 1)
                .orderByAsc(GameTriggerSlot::getSort)
                .list();
    }
}
