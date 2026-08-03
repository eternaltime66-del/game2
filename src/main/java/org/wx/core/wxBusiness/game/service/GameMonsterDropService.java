package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameMonsterDrop;
import org.wx.core.wxBusiness.game.mapper.GameMonsterDropMapper;

import java.util.List;

@Service
public class GameMonsterDropService extends WxServiceImpl<GameMonsterDropMapper, GameMonsterDrop> {

    public List<GameMonsterDrop> listEnabledByMonsterId(String monsterId) {
        return this.find()
                .eq(GameMonsterDrop::getMonsterId, monsterId)
                .eq(GameMonsterDrop::getEnabled, 1)
                .orderByAsc(GameMonsterDrop::getSort)
                .list();
    }
}
