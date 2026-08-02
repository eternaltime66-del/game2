package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameHero;
import org.wx.core.wxBusiness.game.mapper.GameHeroMapper;

@Service
public class GameHeroService extends WxServiceImpl<GameHeroMapper, GameHero> {

    @Transactional(rollbackFor = Exception.class)
    public GameHero initHero(String uid) {
        GameHero exists = findByUid(uid);
        if (exists != null) {
            return exists;
        }
        GameHero hero = GameHero.defaultHero(uid);
        this.save(hero);
        return hero;
    }

    @Transactional(rollbackFor = Exception.class)
    public GameHero getOrInitHero(String uid) {
        GameHero hero = findByUid(uid);
        if (hero == null) {
            hero = initHero(uid);
        }
        return hero;
    }

    public GameHero findByUid(String uid) {
        return this.find().eq(GameHero::getUid, uid).one();
    }
}
