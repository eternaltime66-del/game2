package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.entity.GameCharacterTemplate;
import org.wx.core.wxBusiness.game.entity.GameHero;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameProfession;
import org.wx.core.wxBusiness.game.entity.GameProfessionSkill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameCharacterAdminService {

    @Resource
    private GameCharacterTemplateService templateService;
    @Resource
    private GameHeroService gameHeroService;
    @Resource
    private GamePrepService gamePrepService;
    @Resource
    private GameProfessionService professionService;
    @Resource
    private GameProfessionSkillService professionSkillService;
    @Resource
    private GameItemService gameItemService;

    public List<GameCharacterTemplate> listTemplates() {
        return templateService.find().orderByAsc(GameCharacterTemplate::getCode).list();
    }

    @Transactional(rollbackFor = Exception.class)
    public GameCharacterTemplate saveTemplate(GameCharacterTemplate entity) {
        ErrorFactory.notNull(entity.getCode(), "角色编码不能为空");
        ErrorFactory.notNull(entity.getName(), "角色名称不能为空");
        if (entity.getMaxHp() == null || entity.getMaxHp() < 1) entity.setMaxHp(200);
        if (entity.getAttack() == null) entity.setAttack(10);
        if (entity.getDefense() == null) entity.setDefense(0);
        if (entity.getActionValue() == null || entity.getActionValue() < 1) entity.setActionValue(100);
        if (entity.getEnabled() == null) entity.setEnabled(1);

        GameCharacterTemplate existing = templateService.getByCode(entity.getCode());
        if (existing != null) {
            boolean statsChanged = !eq(existing.getMaxHp(), entity.getMaxHp())
                    || !eq(existing.getAttack(), entity.getAttack())
                    || !eq(existing.getDefense(), entity.getDefense())
                    || !eq(existing.getActionValue(), entity.getActionValue());
            entity.setId(existing.getId());
            int ver = existing.getTemplateVersion() != null ? existing.getTemplateVersion() : 1;
            if (statsChanged) {
                ver += 1;
            }
            entity.setTemplateVersion(ver);
            templateService.updateById(entity);
            return templateService.getById(entity.getId());
        }
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(WordUnit.randomKey(12, 1));
        }
        if (entity.getTemplateVersion() == null) {
            entity.setTemplateVersion(1);
        }
        templateService.save(entity);
        return entity;
    }

    /**
     * 登录加载：同步角色模板基础数值，并按装备重算战斗属性写回场外 HP。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncOwnedCharacters(String uid) {
        Map<String, Object> result = new HashMap<>();
        GameHero hero = gameHeroService.getOrInitHero(uid);
        String code = hero.getTemplateCode() != null ? hero.getTemplateCode() : GameCharacterTemplate.CODE_PROTAGONIST;
        GameCharacterTemplate tpl = templateService.getByCode(code);
        if (tpl == null) {
            tpl = templateService.requireProtagonist();
        }
        int tplVer = tpl.getTemplateVersion() != null ? tpl.getTemplateVersion() : 1;
        int heroVer = hero.getTemplateVersion() != null ? hero.getTemplateVersion() : 0;
        boolean synced = false;
        if (heroVer < tplVer) {
            hero.setName(tpl.getName() != null ? tpl.getName() : hero.getName());
            hero.setTemplateCode(tpl.getCode());
            hero.setMaxHp(tpl.getMaxHp());
            hero.setAttack(tpl.getAttack());
            hero.setDefense(tpl.getDefense());
            hero.setActionValue(tpl.getActionValue());
            hero.setTemplateVersion(tplVer);
            // 基础 maxHp 更新后，先把当前 hp 钳到模板 max，再由 prep 按装备重算
            if (hero.getHp() == null || hero.getHp() > hero.getMaxHp()) {
                hero.setHp(hero.getMaxHp());
            }
            gameHeroService.updateById(hero);
            synced = true;
        }
        // 无论是否升版，都重算装备加成后的场外生命
        int totalMaxHp = gamePrepService.resolveBattleMaxHp(uid);
        gameHeroService.persistOutsideBattleHp(uid, totalMaxHp);
        result.put("synced", synced);
        result.put("templateCode", tpl.getCode());
        result.put("templateVersion", tplVer);
        result.put("totalMaxHp", totalMaxHp);
        result.put("attack", gamePrepService.resolveBattleTotalAttack(uid));
        result.put("defense", gamePrepService.resolveBattleDefense(uid));
        result.put("actionValue", gamePrepService.resolveBattleActionValue(uid));
        return result;
    }

    private boolean eq(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }
}
