package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameFinishedSkillEffect;
import org.wx.core.wxBusiness.game.mapper.GameFinishedSkillEffectMapper;

import java.util.List;

@Service
public class GameFinishedSkillEffectService extends WxServiceImpl<GameFinishedSkillEffectMapper, GameFinishedSkillEffect> {

    public List<GameFinishedSkillEffect> listByFinishedSkillId(String finishedSkillId) {
        return this.find()
                .eq(GameFinishedSkillEffect::getFinishedSkillId, finishedSkillId)
                .orderByAsc(GameFinishedSkillEffect::getSort)
                .list();
    }
}
