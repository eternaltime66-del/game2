package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.mapper.GameCraftMaterialMapper;
import org.wx.core.wxBusiness.game.mapper.GameCraftRecipeMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameCraftService extends WxServiceImpl<GameCraftRecipeMapper, GameCraftRecipe> {

    @Resource
    private GameCraftMaterialMapper craftMaterialMapper;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameArmorService gameArmorService;
    @Resource
    private GameInventoryService inventoryService;

    public List<CraftRecipeVo> listRecipes(String uid) {
        List<GameCraftRecipe> recipes = this.find()
                .eq(GameCraftRecipe::getEnabled, 1)
                .orderByAsc(GameCraftRecipe::getSort)
                .list();
        if (recipes.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> ownedMap = inventoryService.countWarehouseItems(uid);
        return recipes.stream()
                .map(recipe -> buildRecipeVo(recipe, ownedMap))
                .collect(Collectors.toList());
    }

    public CraftRecipeVo getRecipe(String uid, String recipeId) {
        GameCraftRecipe recipe = getEnabledRecipe(recipeId);
        return buildRecipeVo(recipe, inventoryService.countWarehouseItems(uid));
    }

    public List<ItemCraftPreviewVo> listCraftPreviewsByMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        List<GameCraftMaterial> links = craftMaterialMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GameCraftMaterial>()
                        .eq(GameCraftMaterial::getItemId, itemId)
                        .orderByAsc(GameCraftMaterial::getSort));
        if (links.isEmpty()) {
            return List.of();
        }
        Set<String> recipeIds = links.stream().map(GameCraftMaterial::getRecipeId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<ItemCraftPreviewVo> previews = new ArrayList<>();
        for (String recipeId : recipeIds) {
            GameCraftRecipe recipe = this.getById(recipeId);
            if (recipe == null || !Integer.valueOf(1).equals(recipe.getEnabled())) {
                continue;
            }
            GameItem result = gameItemService.getById(recipe.getResultItemId());
            ItemCraftPreviewVo vo = new ItemCraftPreviewVo();
            vo.setCraftId(recipe.getId());
            vo.setName(recipe.getName());
            vo.setRemark(recipe.getRemark());
            vo.setResultItemId(recipe.getResultItemId());
            vo.setResultItemName(result != null ? result.getName() : recipe.getName());
            previews.add(vo);
        }
        return previews;
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public CraftRecipeVo craft(String uid, String recipeId) {
        GameCraftRecipe recipe = getEnabledRecipe(recipeId);
        List<GameCraftMaterial> materials = listMaterials(recipeId);
        ErrorFactory.throwError(materials.isEmpty(), "配方材料未配置");

        Map<String, Integer> ownedMap = inventoryService.countWarehouseItems(uid);
        for (GameCraftMaterial material : materials) {
            int need = material.getQuantity() != null ? material.getQuantity() : 0;
            int owned = ownedMap.getOrDefault(material.getItemId(), 0);
            GameItem item = gameItemService.getById(material.getItemId());
            String itemName = item != null ? item.getName() : material.getItemId();
            ErrorFactory.throwError(owned < need, itemName + " 不足，需要 " + need + "，当前 " + owned);
        }

        for (GameCraftMaterial material : materials) {
            int need = material.getQuantity() != null ? material.getQuantity() : 0;
            inventoryService.consumeWarehouseItem(uid, material.getItemId(), need,
                    GameItemLog.REASON_CRAFT_COST, recipeId, "合成消耗：" + recipe.getName());
        }
        inventoryService.addWarehouseItem(uid, recipe.getResultItemId(), 1,
                GameItemLog.REASON_CRAFT, recipeId, "合成获得：" + recipe.getName());
        return buildRecipeVo(recipe, inventoryService.countWarehouseItems(uid));
    }

    private GameCraftRecipe getEnabledRecipe(String recipeId) {
        GameCraftRecipe recipe = this.getById(recipeId);
        ErrorFactory.notNull(recipe, "配方不存在");
        ErrorFactory.throwError(!Integer.valueOf(1).equals(recipe.getEnabled()), "配方未启用");
        return recipe;
    }

    private List<GameCraftMaterial> listMaterials(String recipeId) {
        return craftMaterialMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GameCraftMaterial>()
                        .eq(GameCraftMaterial::getRecipeId, recipeId)
                        .orderByAsc(GameCraftMaterial::getSort));
    }

    private CraftRecipeVo buildRecipeVo(GameCraftRecipe recipe, Map<String, Integer> ownedMap) {
        CraftRecipeVo vo = new CraftRecipeVo();
        vo.setId(recipe.getId());
        vo.setCode(recipe.getCode());
        vo.setName(recipe.getName());
        vo.setRemark(recipe.getRemark());
        vo.setResultItemId(recipe.getResultItemId());

        GameItem resultItem = gameItemService.getById(recipe.getResultItemId());
        if (resultItem != null) {
            vo.setResultItemName(resultItem.getName());
            vo.setResultItemIcon(resultItem.getIcon());
        }
        GameArmor armor = gameArmorService.getByItemId(recipe.getResultItemId());
        if (armor != null) {
            vo.setArmorBonusHp(armor.getBonusHp());
            vo.setArmorDefense(armor.getDefense());
        }

        List<CraftMaterialVo> materials = new ArrayList<>();
        List<CraftMaterialVo> missingMaterials = new ArrayList<>();
        boolean canCraft = true;
        for (GameCraftMaterial material : listMaterials(recipe.getId())) {
            CraftMaterialVo mv = new CraftMaterialVo();
            mv.setItemId(material.getItemId());
            mv.setRequiredQty(material.getQuantity());
            int owned = ownedMap.getOrDefault(material.getItemId(), 0);
            mv.setOwnedQty(owned);
            int need = material.getQuantity() != null ? material.getQuantity() : 0;
            int missing = Math.max(0, need - owned);
            mv.setMissingQty(missing);
            mv.setEnough(missing <= 0);
            GameItem item = gameItemService.getById(material.getItemId());
            if (item != null) {
                mv.setItemName(item.getName());
                mv.setIcon(item.getIcon());
            }
            materials.add(mv);
            if (missing > 0) {
                missingMaterials.add(mv);
                canCraft = false;
            }
        }
        vo.setMaterials(materials);
        vo.setMissingMaterials(missingMaterials);
        vo.setCanCraft(canCraft);
        return vo;
    }
}
