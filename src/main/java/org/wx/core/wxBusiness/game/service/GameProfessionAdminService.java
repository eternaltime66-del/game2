package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameProfession;
import org.wx.core.wxBusiness.game.entity.GameProfessionSkill;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameProfessionAdminService {

    @Resource
    private GameProfessionService professionService;
    @Resource
    private GameProfessionSkillService professionSkillService;
    @Resource
    private GameItemService gameItemService;

    public List<GameProfession> listProfessions() {
        List<GameProfession> list = professionService.find()
                .orderByAsc(GameProfession::getSort)
                .orderByAsc(GameProfession::getCode)
                .list();
        for (GameProfession p : list) {
            long cnt = professionSkillService.find()
                    .eq(GameProfessionSkill::getProfessionId, p.getId())
                    .count();
            p.setSkillCount((int) cnt);
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveProfession(GameProfession entity) {
        ErrorFactory.notNull(entity.getCode(), "职业编码不能为空");
        ErrorFactory.notNull(entity.getName(), "职业名称不能为空");
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getEnabled() == null) entity.setEnabled(1);
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(WordUnit.randomKey(12, 1));
            professionService.save(entity);
        } else {
            professionService.updateById(entity);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeProfession(String id) {
        ErrorFactory.notNull(id, "职业ID不能为空");
        List<GameProfessionSkill> skills = professionSkillService.find()
                .eq(GameProfessionSkill::getProfessionId, id)
                .list();
        for (GameProfessionSkill s : skills) {
            professionSkillService.removeById(s.getId());
        }
        professionService.removeById(id);
    }

    public List<GameProfessionSkill> listProfessionSkills(String professionId) {
        ErrorFactory.notNull(professionId, "职业ID不能为空");
        List<GameProfessionSkill> rows = professionSkillService.find()
                .eq(GameProfessionSkill::getProfessionId, professionId)
                .orderByAsc(GameProfessionSkill::getSort)
                .list();
        if (rows.isEmpty()) return rows;
        List<String> itemIds = rows.stream().map(GameProfessionSkill::getItemId).distinct().toList();
        Map<String, GameItem> map = gameItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(GameItem::getId, i -> i, (a, b) -> a));
        for (GameProfessionSkill row : rows) {
            GameItem item = map.get(row.getItemId());
            if (item != null) {
                row.setItemName(item.getName());
                row.setItemCode(item.getCode());
            }
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveProfessionSkill(GameProfessionSkill entity) {
        ErrorFactory.notNull(entity.getProfessionId(), "职业不能为空");
        ErrorFactory.notNull(entity.getItemId(), "技能物品不能为空");
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getEnabled() == null) entity.setEnabled(1);
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(WordUnit.randomKey(12, 1));
            professionSkillService.save(entity);
        } else {
            professionSkillService.updateById(entity);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeProfessionSkill(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        professionSkillService.removeById(id);
    }
}
