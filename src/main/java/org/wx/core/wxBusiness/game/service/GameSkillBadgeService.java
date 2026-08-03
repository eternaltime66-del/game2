package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameSkillBadge;
import org.wx.core.wxBusiness.game.mapper.GameSkillBadgeMapper;

@Service
public class GameSkillBadgeService extends WxServiceImpl<GameSkillBadgeMapper, GameSkillBadge> {

    public GameSkillBadge getByItemId(String itemId) {
        return getById(itemId);
    }
}
