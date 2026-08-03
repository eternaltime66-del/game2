package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.MaterialSourceType;

@Service
public class GameMaterialSourceService {

    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameRecipeService recipeService;
    @Resource
    private GameStageDropService stageDropService;
    @Resource
    private GameLevelService gameLevelService;
    @Resource
    private GameStageGroupService stageGroupService;
    @Resource
    private GameItemDropSourceService itemDropSourceService;

    /**
     * 查询材料获取来源：前往制作 / 前往出击 / 暂无来源
     */
    public MaterialSourceVo resolveSource(String itemId) {
        return resolveDisplaySource(itemId);
    }

    /**
     * 合成台材料行展示：优先掉落，其次合成，否则敬请期待
     */
    public MaterialSourceVo resolveDisplaySource(String itemId) {
        ErrorFactory.notNull(itemId, "物品ID不能为空");
        GameItem item = gameItemService.getById(itemId);
        ErrorFactory.notNull(item, "物品不存在");

        MaterialSourceVo vo = new MaterialSourceVo();
        vo.setItemId(itemId);

        var dropSources = itemDropSourceService.listByItemId(itemId);
        if (!dropSources.isEmpty()) {
            vo.setSourceType(MaterialSourceType.BATTLE.getCode());
            vo.setLabel("掉落");
            GameStageDrop drop = stageDropService.find()
                    .eq(GameStageDrop::getItemId, itemId)
                    .eq(GameStageDrop::getEnabled, 1)
                    .orderByAsc(GameStageDrop::getId)
                    .one();
            if (drop != null) {
                vo.setStageId(drop.getStageId());
                fillStageInfo(vo, drop.getStageId());
            } else {
                ItemDropSourceVo first = dropSources.get(0);
                vo.setStageId(first.getStageId());
                vo.setStageName(first.getStageName());
                vo.setStageDisplayCode(first.getDisplayCode());
            }
            return vo;
        }

        GameRecipe recipe = recipeService.find()
                .eq(GameRecipe::getOutputItemId, itemId)
                .eq(GameRecipe::getEnabled, 1)
                .orderByAsc(GameRecipe::getSort)
                .one();
        if (recipe != null) {
            vo.setSourceType(MaterialSourceType.CRAFT.getCode());
            vo.setLabel("去合成");
            vo.setRecipeId(recipe.getId());
            return vo;
        }

        vo.setSourceType(MaterialSourceType.NONE.getCode());
        vo.setLabel("敬请期待");
        return vo;
    }

    private void fillStageInfo(MaterialSourceVo vo, String stageId) {
        GameStage stage = gameLevelService.getById(stageId);
        if (stage == null) {
            return;
        }
        vo.setStageName(stage.getName());
        if (stage.getStageGroupId() != null) {
            GameStageGroup group = stageGroupService.getById(stage.getStageGroupId());
            if (group != null && group.getGroupNo() != null && stage.getStageNo() != null) {
                vo.setStageDisplayCode(group.getGroupNo() + "-" + stage.getStageNo());
            }
        }
    }
}
