package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameCompleteSkill;
import org.wx.core.wxBusiness.game.mapper.GameCompleteSkillMapper;

import java.util.List;

@Service
public class GameCompleteSkillService extends WxServiceImpl<GameCompleteSkillMapper, GameCompleteSkill> {

    public List<GameCompleteSkill> listEnabled() {
        return this.find()
                .eq(GameCompleteSkill::getEnabled, 1)
                .orderByAsc(GameCompleteSkill::getSort)
                .list();
    }
}
