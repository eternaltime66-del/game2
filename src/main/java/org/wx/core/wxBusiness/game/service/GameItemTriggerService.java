package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameItemTrigger;
import org.wx.core.wxBusiness.game.entity.enums.GameTriggerPhase;
import org.wx.core.wxBusiness.game.mapper.GameItemTriggerMapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class GameItemTriggerService extends WxServiceImpl<GameItemTriggerMapper, GameItemTrigger> {

    public List<GameItemTrigger> listEnabledByItemIds(Collection<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.find()
                .in(GameItemTrigger::getItemId, itemIds)
                .eq(GameItemTrigger::getEnabled, 1)
                .orderByAsc(GameItemTrigger::getSort)
                .list();
    }

    public List<GameItemTrigger> listEnabledByItemIdsAndPhase(Collection<String> itemIds, GameTriggerPhase phase) {
        if (itemIds == null || itemIds.isEmpty() || phase == null) {
            return Collections.emptyList();
        }
        return this.find()
                .in(GameItemTrigger::getItemId, itemIds)
                .eq(GameItemTrigger::getTriggerPhase, phase.name())
                .eq(GameItemTrigger::getEnabled, 1)
                .orderByAsc(GameItemTrigger::getSort)
                .list();
    }
}
