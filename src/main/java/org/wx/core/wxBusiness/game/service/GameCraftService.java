package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameCraftService {

    @Resource
    private GameRecipeService recipeService;
    @Resource
    private GameRecipeMaterialService recipeMaterialService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameArmorService gameArmorService;
    @Resource
    private GameInventoryService inventoryService;
    @Resource
    private GameMaterialSourceService materialSourceService;
    @Resource
    private GameBattleBagService battleBagService;

    public List<CraftRecipeVo> listRecipes(String uid) {
        List<GameRecipe> recipes = recipeService.find()
                .eq(GameRecipe::getEnabled, 1)
                .orderByAsc(GameRecipe::getSort)
                .orderByAsc(GameRecipe::getId)
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
        GameRecipe recipe = getEnabledRecipe(recipeId);
        return buildRecipeVo(recipe, inventoryService.countWarehouseItems(uid));
    }

    public List<ItemCraftPreviewVo> listCraftPreviewsByMaterial(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        List<GameRecipeMaterial> links = recipeMaterialService.find()
                .eq(GameRecipeMaterial::getMaterialItemId, itemId)
                .orderByAsc(GameRecipeMaterial::getSort)
                .list();
        if (links.isEmpty()) {
            return List.of();
        }
        Set<String> recipeIds = links.stream()
                .map(GameRecipeMaterial::getRecipeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ItemCraftPreviewVo> previews = new ArrayList<>();
        for (String recipeId : recipeIds) {
            GameRecipe recipe = recipeService.getById(recipeId);
            if (recipe == null || !Integer.valueOf(1).equals(recipe.getEnabled())) {
                continue;
            }
            GameItem result = gameItemService.getById(recipe.getOutputItemId());
            ItemCraftPreviewVo vo = new ItemCraftPreviewVo();
            vo.setCraftId(recipe.getId());
            vo.setName(result != null ? result.getName() : recipe.getId());
            vo.setRemark(recipe.getRemark());
            vo.setResultItemId(recipe.getOutputItemId());
            vo.setResultItemName(result != null ? result.getName() : recipe.getId());
            previews.add(vo);
        }
        return previews;
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public CraftRecipeVo craft(String uid, String recipeId) {
        GameRecipe recipe = getEnabledRecipe(recipeId);
        List<GameRecipeMaterial> materials = listMaterials(recipeId);
        ErrorFactory.throwError(materials.isEmpty(), "配方材料未配置");

        Map<String, Integer> ownedMap = inventoryService.countWarehouseItems(uid);
        for (GameRecipeMaterial material : materials) {
            int need = material.getQuantity() != null ? material.getQuantity() : 0;
            int owned = ownedMap.getOrDefault(material.getMaterialItemId(), 0);
            GameItem item = gameItemService.getById(material.getMaterialItemId());
            String itemName = item != null ? item.getName() : material.getMaterialItemId();
            ErrorFactory.throwError(owned < need, itemName + " 不足，需要 " + need + "，当前 " + owned);
        }

        String recipeName = resolveRecipeName(recipe);
        for (GameRecipeMaterial material : materials) {
            int need = material.getQuantity() != null ? material.getQuantity() : 0;
            inventoryService.consumeWarehouseItem(uid, material.getMaterialItemId(), need,
                    GameItemLog.REASON_CRAFT_COST, recipeId, "合成消耗：" + recipeName);
        }
        GameItem outputItem = gameItemService.getById(recipe.getOutputItemId());
        String outputName = outputItem != null ? outputItem.getName() : recipe.getOutputItemId();
        int beforeQty = battleBagService.grantQuantity(uid, recipe.getOutputItemId(), 1);
        inventoryService.saveItemLog(uid, recipe.getOutputItemId(), outputName, 1, beforeQty, beforeQty + 1,
                GameItemLog.REASON_CRAFT, recipeId, "合成获得：" + recipeName);
        return buildRecipeVo(recipe, inventoryService.countWarehouseItems(uid));
    }

    private GameRecipe getEnabledRecipe(String recipeId) {
        GameRecipe recipe = recipeService.getById(recipeId);
        ErrorFactory.notNull(recipe, "配方不存在");
        ErrorFactory.throwError(!Integer.valueOf(1).equals(recipe.getEnabled()), "配方未启用");
        return recipe;
    }

    private List<GameRecipeMaterial> listMaterials(String recipeId) {
        return recipeMaterialService.find()
                .eq(GameRecipeMaterial::getRecipeId, recipeId)
                .orderByAsc(GameRecipeMaterial::getSort)
                .list();
    }

    private String resolveRecipeName(GameRecipe recipe) {
        GameItem resultItem = gameItemService.getById(recipe.getOutputItemId());
        return resultItem != null ? resultItem.getName() : recipe.getId();
    }

    private CraftRecipeVo buildRecipeVo(GameRecipe recipe, Map<String, Integer> ownedMap) {
        CraftRecipeVo vo = new CraftRecipeVo();
        vo.setId(recipe.getId());
        vo.setRemark(recipe.getRemark());
        vo.setResultItemId(recipe.getOutputItemId());

        GameItem resultItem = gameItemService.getById(recipe.getOutputItemId());
        if (resultItem != null) {
            vo.setCode(resultItem.getCode());
            vo.setName(resultItem.getName());
            vo.setResultItemName(resultItem.getName());
            vo.setResultItemIcon(resultItem.getIcon());
        } else {
            vo.setName(recipe.getId());
            vo.setResultItemName(recipe.getId());
        }
        GameArmor armor = gameArmorService.getByItemId(recipe.getOutputItemId());
        if (armor != null) {
            vo.setArmorBonusHp(armor.getBonusHp());
            vo.setArmorDefense(armor.getDefense());
        }

        List<CraftMaterialVo> materials = new ArrayList<>();
        List<CraftMaterialVo> missingMaterials = new ArrayList<>();
        boolean canCraft = true;
        for (GameRecipeMaterial material : listMaterials(recipe.getId())) {
            CraftMaterialVo mv = new CraftMaterialVo();
            mv.setItemId(material.getMaterialItemId());
            mv.setRequiredQty(material.getQuantity());
            int owned = ownedMap.getOrDefault(material.getMaterialItemId(), 0);
            mv.setOwnedQty(owned);
            int need = material.getQuantity() != null ? material.getQuantity() : 0;
            int missing = Math.max(0, need - owned);
            mv.setMissingQty(missing);
            mv.setEnough(missing <= 0);
            GameItem item = gameItemService.getById(material.getMaterialItemId());
            if (item != null) {
                mv.setItemName(item.getName());
                mv.setIcon(item.getIcon());
            }
            MaterialSourceVo source = materialSourceService.resolveDisplaySource(material.getMaterialItemId());
            mv.setSourceType(source.getSourceType());
            mv.setSourceLabel(source.getLabel());
            mv.setSourceRecipeId(source.getRecipeId());
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
