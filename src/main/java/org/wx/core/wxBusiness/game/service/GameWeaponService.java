package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameWeapon;
import org.wx.core.wxBusiness.game.mapper.GameWeaponMapper;

@Service
public class GameWeaponService extends WxServiceImpl<GameWeaponMapper, GameWeapon> {

    public GameWeapon getByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return this.find()
                .eq(GameWeapon::getItemId, itemId)
                .eq(GameWeapon::getEnabled, 1)
                .one();
    }
}
