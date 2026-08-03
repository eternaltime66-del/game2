package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.factory.PageFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.mapper.GameRecipeMaterialMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameRecipeAdminService {

    @Resource
    private GameRecipeService recipeService;
    @Resource
    private GameRecipeMaterialService recipeMaterialService;
    @Resource
    private GameRecipeMaterialMapper recipeMaterialMapper;
    @Resource
    private GameItemService gameItemService;

    public IPage<AdminRecipeVo> listRecipes(GameRecipe query) {
        List<GameRecipe> recipes = recipeService.find()
                .orderByAsc(GameRecipe::getSort)
                .orderByAsc(GameRecipe::getId)
                .list();
        List<AdminRecipeVo> vos = recipes.stream()
                .map(this::buildRecipeListVo)
                .collect(Collectors.toList());
        return sliceVo(vos, query);
    }

    public AdminRecipeVo getRecipeDetail(String id) {
        GameRecipe recipe = recipeService.getById(id);
        ErrorFactory.notNull(recipe, "配方不存在");
        return buildRecipeDetailVo(recipe);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminRecipeVo saveRecipe(AdminRecipeVo vo) {
        ErrorFactory.notNull(vo.getOutputItemId(), "产出物品不能为空");

        GameItem outputItem = gameItemService.getById(vo.getOutputItemId());
        ErrorFactory.notNull(outputItem, "产出物品不存在");

        List<AdminRecipeMaterialVo> materials = vo.getMaterials() != null ? vo.getMaterials() : List.of();
        ErrorFactory.throwError(materials.isEmpty(), "至少添加一种材料");

        Set<String> materialIds = new HashSet<>();
        for (AdminRecipeMaterialVo mat : materials) {
            ErrorFactory.notNull(mat.getMaterialItemId(), "材料不能为空");
            ErrorFactory.throwError(mat.getMaterialItemId().equals(vo.getOutputItemId()), "材料不能与产出物品相同");
            ErrorFactory.throwError(!materialIds.add(mat.getMaterialItemId()), "材料不能重复");
            GameItem materialItem = gameItemService.getById(mat.getMaterialItemId());
            ErrorFactory.notNull(materialItem, "材料物品不存在: " + mat.getMaterialItemId());
            int qty = mat.getQuantity() != null ? mat.getQuantity() : 1;
            ErrorFactory.throwError(qty <= 0, "材料数量必须大于 0");
        }

        GameRecipe entity = new GameRecipe();
        entity.setId(vo.getId());
        entity.setOutputItemId(vo.getOutputItemId());
        entity.setSort(vo.getSort() != null ? vo.getSort() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        entity.setRemark(vo.getRemark());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId("rcp_" + outputItem.getCode().toLowerCase());
            if (recipeService.getById(entity.getId()) != null) {
                entity.setId(entity.getId() + "_" + System.currentTimeMillis());
            }
            long dup = recipeService.count(new LambdaQueryWrapper<GameRecipe>()
                    .eq(GameRecipe::getOutputItemId, entity.getOutputItemId()));
            ErrorFactory.throwError(dup != 0, "该产出物品已有配方");
            recipeService.save(entity);
        } else {
            GameRecipe existing = recipeService.getById(entity.getId());
            ErrorFactory.notNull(existing, "配方不存在");
            if (!existing.getOutputItemId().equals(entity.getOutputItemId())) {
                long dup = recipeService.count(new LambdaQueryWrapper<GameRecipe>()
                        .eq(GameRecipe::getOutputItemId, entity.getOutputItemId())
                        .ne(GameRecipe::getId, entity.getId()));
                ErrorFactory.throwError(dup != 0, "该产出物品已有配方");
            }
            recipeService.updateById(entity);
        }

        recipeMaterialMapper.delete(new LambdaQueryWrapper<GameRecipeMaterial>()
                .eq(GameRecipeMaterial::getRecipeId, entity.getId()));

        int sort = 1;
        for (AdminRecipeMaterialVo matVo : materials) {
            GameRecipeMaterial mat = new GameRecipeMaterial();
            mat.setId(matVo.getId() != null && !matVo.getId().isBlank()
                    ? matVo.getId() : entity.getId() + "_mat_" + sort);
            mat.setRecipeId(entity.getId());
            mat.setMaterialItemId(matVo.getMaterialItemId());
            mat.setQuantity(matVo.getQuantity() != null ? matVo.getQuantity() : 1);
            mat.setSort(matVo.getSort() != null ? matVo.getSort() : sort);
            recipeMaterialService.save(mat);
            sort++;
        }
        return buildRecipeDetailVo(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeRecipe(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        recipeMaterialMapper.delete(new LambdaQueryWrapper<GameRecipeMaterial>()
                .eq(GameRecipeMaterial::getRecipeId, id));
        recipeService.removeById(id);
    }

    private AdminRecipeVo buildRecipeListVo(GameRecipe entity) {
        AdminRecipeVo vo = new AdminRecipeVo();
        vo.setId(entity.getId());
        vo.setOutputItemId(entity.getOutputItemId());
        GameItem output = gameItemService.getById(entity.getOutputItemId());
        if (output != null) {
            vo.setOutputItemName(output.getName());
        }
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setMaterialSummary(buildMaterialSummary(entity.getId()));
        return vo;
    }

    private AdminRecipeVo buildRecipeDetailVo(GameRecipe entity) {
        AdminRecipeVo vo = buildRecipeListVo(entity);
        List<GameRecipeMaterial> mats = recipeMaterialService.find()
                .eq(GameRecipeMaterial::getRecipeId, entity.getId())
                .orderByAsc(GameRecipeMaterial::getSort)
                .list();
        List<AdminRecipeMaterialVo> matVos = new ArrayList<>();
        for (GameRecipeMaterial mat : mats) {
            AdminRecipeMaterialVo matVo = new AdminRecipeMaterialVo();
            matVo.setId(mat.getId());
            matVo.setMaterialItemId(mat.getMaterialItemId());
            GameItem item = gameItemService.getById(mat.getMaterialItemId());
            if (item != null) {
                matVo.setMaterialItemName(item.getName());
            }
            matVo.setQuantity(mat.getQuantity());
            matVo.setSort(mat.getSort());
            matVos.add(matVo);
        }
        vo.setMaterials(matVos);
        return vo;
    }

    private String buildMaterialSummary(String recipeId) {
        List<GameRecipeMaterial> mats = recipeMaterialService.find()
                .eq(GameRecipeMaterial::getRecipeId, recipeId)
                .orderByAsc(GameRecipeMaterial::getSort)
                .list();
        if (mats.isEmpty()) {
            return "-";
        }
        return mats.stream()
                .map(mat -> {
                    GameItem item = gameItemService.getById(mat.getMaterialItemId());
                    String name = item != null ? item.getName() : mat.getMaterialItemId();
                    return name + "×" + mat.getQuantity();
                })
                .collect(Collectors.joining("、"));
    }

    private <T> IPage<T> sliceVo(List<T> all, Object ignored) {
        Page<T> page = PageFactory.defaultPage();
        int current = (int) page.getCurrent();
        int size = (int) page.getSize();
        int from = Math.max(0, (current - 1) * size);
        int to = Math.min(all.size(), from + size);
        List<T> pageRecords = from >= all.size() ? List.of() : all.subList(from, to);
        page.setTotal(all.size());
        page.setRecords(pageRecords);
        return page;
    }
}
