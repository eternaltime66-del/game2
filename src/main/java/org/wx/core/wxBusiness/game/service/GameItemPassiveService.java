package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameItemPassive;
import org.wx.core.wxBusiness.game.mapper.GameItemPassiveMapper;

import java.util.Collections;
import java.util.List;

@Service
public class GameItemPassiveService extends WxServiceImpl<GameItemPassiveMapper, GameItemPassive> {

    public List<GameItemPassive> listByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Collections.emptyList();
        }
        return find()
                .eq(GameItemPassive::getItemId, itemId)
                .orderByAsc(GameItemPassive::getSort)
                .list();
    }

    public List<GameItemPassive> listEnabledByItemIds(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return find()
                .in(GameItemPassive::getItemId, itemIds)
                .eq(GameItemPassive::getEnabled, 1)
                .orderByAsc(GameItemPassive::getSort)
                .list();
    }
}
