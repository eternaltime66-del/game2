package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameCharacterTemplate;
import org.wx.core.wxBusiness.game.mapper.GameCharacterTemplateMapper;

@Service
public class GameCharacterTemplateService extends WxServiceImpl<GameCharacterTemplateMapper, GameCharacterTemplate> {

    public GameCharacterTemplate getByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return this.find().eq(GameCharacterTemplate::getCode, code).one();
    }

    public GameCharacterTemplate requireProtagonist() {
        GameCharacterTemplate tpl = getByCode(GameCharacterTemplate.CODE_PROTAGONIST);
        if (tpl == null) {
            tpl = new GameCharacterTemplate();
            tpl.setId("char_tpl_protagonist");
            tpl.setCode(GameCharacterTemplate.CODE_PROTAGONIST);
            tpl.setName("主角");
            tpl.setMaxHp(GameHeroDefaults.MAX_HP);
            tpl.setAttack(GameHeroDefaults.ATTACK);
            tpl.setDefense(GameHeroDefaults.DEFENSE);
            tpl.setActionValue(GameHeroDefaults.ACTION_VALUE);
            tpl.setTemplateVersion(1);
            tpl.setEnabled(1);
            this.save(tpl);
        }
        return tpl;
    }

    /** 避免循环依赖用的常量 */
    private static final class GameHeroDefaults {
        static final int MAX_HP = 200;
        static final int ATTACK = 10;
        static final int DEFENSE = 0;
        static final int ACTION_VALUE = 100;
    }
}
