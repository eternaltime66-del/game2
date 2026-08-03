package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameMonsterPassive;
import org.wx.core.wxBusiness.game.mapper.GameMonsterPassiveMapper;

import java.util.Collections;
import java.util.List;

@Service
public class GameMonsterPassiveService extends WxServiceImpl<GameMonsterPassiveMapper, GameMonsterPassive> {

    public List<GameMonsterPassive> listByMonsterId(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return Collections.emptyList();
        }
        return this.find()
                .eq(GameMonsterPassive::getMonsterId, monsterId)
                .orderByAsc(GameMonsterPassive::getSort)
                .list();
    }

    public List<GameMonsterPassive> listEnabledByMonsterId(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return Collections.emptyList();
        }
        return this.find()
                .eq(GameMonsterPassive::getMonsterId, monsterId)
                .eq(GameMonsterPassive::getEnabled, 1)
                .orderByAsc(GameMonsterPassive::getSort)
                .list();
    }
}
