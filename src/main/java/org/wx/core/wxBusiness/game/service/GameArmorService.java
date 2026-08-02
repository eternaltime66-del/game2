package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameArmor;
import org.wx.core.wxBusiness.game.mapper.GameArmorMapper;

@Service
public class GameArmorService extends WxServiceImpl<GameArmorMapper, GameArmor> {

    public GameArmor getByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return this.find()
                .eq(GameArmor::getItemId, itemId)
                .eq(GameArmor::getEnabled, 1)
                .one();
    }
}
