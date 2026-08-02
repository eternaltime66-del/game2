package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameWarehouse;
import org.wx.core.wxBusiness.game.mapper.GameWarehouseMapper;

@Service
public class GameWarehouseService extends WxServiceImpl<GameWarehouseMapper, GameWarehouse> {

    @Transactional(rollbackFor = Exception.class)
    public GameWarehouse getOrInit(String uid) {
        GameWarehouse warehouse = this.find().eq(GameWarehouse::getUid, uid).one();
        if (warehouse != null) {
            return warehouse;
        }
        warehouse = new GameWarehouse();
        warehouse.setUid(uid);
        warehouse.setMaxSlots(GameWarehouse.DEFAULT_MAX_SLOTS);
        this.save(warehouse);
        return warehouse;
    }
}
