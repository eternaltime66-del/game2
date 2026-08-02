package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameItemTrigger;
import org.wx.core.wxBusiness.game.entity.GameSkill;
import org.wx.core.wxBusiness.game.entity.TriggerOptionVo;
import org.wx.core.wxBusiness.game.entity.enums.GameTriggerPhase;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameItemTriggerAdminService {

    @Resource
    private GameItemTriggerService triggerService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameSkillService skillService;

    public IPage<GameItemTrigger> list(GameItemTrigger query) {
        return triggerService.pageQuery(query);
    }

    public List<GameItemTrigger> listByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        return triggerService.find()
                .eq(GameItemTrigger::getItemId, itemId)
                .orderByAsc(GameItemTrigger::getSort)
                .list();
    }

    @Transactional(rollbackFor = Exception.class)
    public GameItemTrigger save(GameItemTrigger entity) {
        entity.clearEmptyString();
        ErrorFactory.notNull(entity.getItemId(), "物品ID不能为空");
        ErrorFactory.notNull(entity.getTriggerPhase(), "扳机时机不能为空");
        ErrorFactory.notNull(entity.getSkillId(), "完整技能ID不能为空");

        GameTriggerPhase phase = GameTriggerPhase.parse(entity.getTriggerPhase());
        ErrorFactory.notNull(phase, "扳机时机无效");

        GameItem item = gameItemService.getById(entity.getItemId());
        ErrorFactory.notNull(item, "物品不存在");

        GameSkill skill = skillService.getById(entity.getSkillId());
        ErrorFactory.notNull(skill, "技能不存在");

        if (phase.isCounter()) {
            ErrorFactory.notNull(entity.getThresholdValue(), "累计类扳机必须设置阈值");
            ErrorFactory.throwError(entity.getThresholdValue().compareTo(BigDecimal.ZERO) <= 0, "阈值必须大于0");
        } else {
            entity.setThresholdValue(null);
        }

        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }

        entity.setTriggerPhase(phase.name());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(buildTriggerId(entity.getItemId(), phase));
            triggerService.save(entity);
        } else {
            triggerService.updateById(entity);
        }
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        triggerService.removeById(id);
    }

    public List<TriggerOptionVo> listPhaseOptions() {
        return GameTriggerPhase.allSorted().stream()
                .map(phase -> toOption(phase.name(), phase.getLabel(), phase.getSort()))
                .collect(Collectors.toList());
    }

    public Map<String, String> itemNameMap(Collection<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }
        return gameItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(GameItem::getId, GameItem::getName, (a, b) -> a));
    }

    private String buildTriggerId(String itemId, GameTriggerPhase phase) {
        String base = "trg_" + itemId.replace("item_", "") + "_" + phase.name().toLowerCase();
        if (triggerService.getById(base) == null) {
            return base;
        }
        return base + "_" + System.currentTimeMillis();
    }

    private TriggerOptionVo toOption(String code, String label, int sort) {
        TriggerOptionVo vo = new TriggerOptionVo();
        vo.setCode(code);
        vo.setLabel(label);
        vo.setSort(sort);
        return vo;
    }
}
