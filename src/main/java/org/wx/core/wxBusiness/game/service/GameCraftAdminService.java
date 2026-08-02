package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.mapper.GameCraftMaterialMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameCraftAdminService {

    @Resource
    private GameCraftService craftRecipeService;
    @Resource
    private GameCraftMaterialMapper craftMaterialMapper;
    @Resource
    private GameItemService gameItemService;

    public IPage<GameCraftRecipe> listRecipes(GameCraftRecipe query) {
        return craftRecipeService.pageQuery(query);
    }

    public AdminCraftRecipeVo getDetail(String recipeId) {
        GameCraftRecipe recipe = craftRecipeService.getById(recipeId);
        ErrorFactory.notNull(recipe, "配方不存在");
        return buildAdminVo(recipe);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCraftRecipeVo saveRecipe(AdminCraftRecipeVo vo) {
        ErrorFactory.notNull(vo, "配方不能为空");
        ErrorFactory.notNull(vo.getCode(), "配方编码不能为空");
        ErrorFactory.notNull(vo.getName(), "配方名称不能为空");
        ErrorFactory.notNull(vo.getResultItemId(), "产物物品不能为空");

        GameItem resultItem = gameItemService.getById(vo.getResultItemId());
        ErrorFactory.notNull(resultItem, "产物物品不存在");

        GameCraftRecipe recipe = new GameCraftRecipe();
        recipe.setId(vo.getId());
        recipe.setCode(vo.getCode().trim().toUpperCase());
        recipe.setName(vo.getName().trim());
        recipe.setResultItemId(vo.getResultItemId());
        recipe.setSort(vo.getSort() != null ? vo.getSort() : 0);
        recipe.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        recipe.setRemark(vo.getRemark());

        if (recipe.getId() == null || recipe.getId().isBlank()) {
            recipe.setId("craft_" + recipe.getCode().toLowerCase());
            craftRecipeService.save(recipe);
        } else {
            craftRecipeService.updateById(recipe);
        }

        craftMaterialMapper.delete(new LambdaQueryWrapper<GameCraftMaterial>()
                .eq(GameCraftMaterial::getRecipeId, recipe.getId()));

        List<AdminCraftMaterialVo> materials = vo.getMaterials() != null ? vo.getMaterials() : List.of();
        int sort = 1;
        for (AdminCraftMaterialVo materialVo : materials) {
            if (materialVo == null || materialVo.getItemId() == null || materialVo.getItemId().isBlank()) {
                continue;
            }
            GameItem item = gameItemService.getById(materialVo.getItemId());
            ErrorFactory.notNull(item, "材料物品不存在: " + materialVo.getItemId());

            int qty = materialVo.getQuantity() != null ? materialVo.getQuantity() : 1;
            ErrorFactory.throwError(qty <= 0, "材料数量必须大于0");

            GameCraftMaterial material = new GameCraftMaterial();
            String materialId = materialVo.getId();
            if (materialId == null || materialId.isBlank()) {
                materialId = recipe.getId() + "_mat_" + sort;
            }
            material.setId(materialId);
            material.setRecipeId(recipe.getId());
            material.setItemId(materialVo.getItemId());
            material.setQuantity(qty);
            material.setSort(materialVo.getSort() != null ? materialVo.getSort() : sort);
            craftMaterialMapper.insert(material);
            sort++;
        }

        return getDetail(recipe.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecipe(String recipeId) {
        ErrorFactory.notNull(recipeId, "配方ID不能为空");
        craftMaterialMapper.delete(new LambdaQueryWrapper<GameCraftMaterial>()
                .eq(GameCraftMaterial::getRecipeId, recipeId));
        craftRecipeService.removeById(recipeId);
    }

    public List<ItemTagOptionVo> listItemTagOptions() {
        return GameItemTag.allSorted().stream()
                .map(tag -> {
                    ItemTagOptionVo option = new ItemTagOptionVo();
                    option.setCode(tag.name());
                    option.setLabel(tag.getLabel());
                    option.setSort(tag.getSort());
                    return option;
                })
                .collect(Collectors.toList());
    }

    private AdminCraftRecipeVo buildAdminVo(GameCraftRecipe recipe) {
        AdminCraftRecipeVo vo = new AdminCraftRecipeVo();
        vo.setId(recipe.getId());
        vo.setCode(recipe.getCode());
        vo.setName(recipe.getName());
        vo.setResultItemId(recipe.getResultItemId());
        vo.setSort(recipe.getSort());
        vo.setEnabled(recipe.getEnabled());
        vo.setRemark(recipe.getRemark());

        GameItem resultItem = gameItemService.getById(recipe.getResultItemId());
        if (resultItem != null) {
            vo.setResultItemName(resultItem.getName());
        }

        List<GameCraftMaterial> materials = craftMaterialMapper.selectList(
                new LambdaQueryWrapper<GameCraftMaterial>()
                        .eq(GameCraftMaterial::getRecipeId, recipe.getId())
                        .orderByAsc(GameCraftMaterial::getSort));
        if (materials.isEmpty()) {
            return vo;
        }

        Set<String> itemIds = materials.stream().map(GameCraftMaterial::getItemId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, GameItem> itemMap = gameItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(GameItem::getId, i -> i, (a, b) -> a));

        List<AdminCraftMaterialVo> materialVos = new ArrayList<>();
        for (GameCraftMaterial material : materials) {
            AdminCraftMaterialVo mv = new AdminCraftMaterialVo();
            mv.setId(material.getId());
            mv.setRecipeId(material.getRecipeId());
            mv.setItemId(material.getItemId());
            mv.setQuantity(material.getQuantity());
            mv.setSort(material.getSort());
            GameItem item = itemMap.get(material.getItemId());
            if (item != null) {
                mv.setItemName(item.getName());
            }
            materialVos.add(mv);
        }
        vo.setMaterials(materialVos);
        return vo;
    }
}
