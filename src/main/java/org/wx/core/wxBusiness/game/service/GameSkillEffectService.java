package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameSkillEffect;
import org.wx.core.wxBusiness.game.mapper.GameSkillEffectMapper;

import java.util.List;

@Service
public class GameSkillEffectService extends WxServiceImpl<GameSkillEffectMapper, GameSkillEffect> {

    public List<GameSkillEffect> listBySkillId(String skillId) {
        return this.find()
                .eq(GameSkillEffect::getSkillId, skillId)
                .orderByAsc(GameSkillEffect::getSort)
                .list();
    }
}
