package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.GameArmor;
import org.wx.core.wxBusiness.game.entity.GameCraftMaterial;
import org.wx.core.wxBusiness.game.entity.GameCraftRecipe;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameWeapon;
import org.wx.core.wxBusiness.game.entity.ItemCraftPreviewVo;
import org.wx.core.wxBusiness.game.entity.ItemDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemTagHelper;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.mapper.GameCraftMaterialMapper;
import org.wx.core.wxBusiness.game.mapper.GameCraftRecipeMapper;
import org.wx.core.wxBusiness.game.mapper.GameItemMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameItemService extends WxServiceImpl<GameItemMapper, GameItem> {

    @Resource
    private GameWeaponService gameWeaponService;
    @Resource
    private GameArmorService gameArmorService;
    @Resource
    private GameCraftMaterialMapper craftMaterialMapper;
    @Resource
    private GameCraftRecipeMapper craftRecipeMapper;

    public ItemDetailVo getItemDetail(String itemId) {
        GameItem item = this.getById(itemId);
        ErrorFactory.notNull(item, "物品不存在");
        ErrorFactory.throwError(!Integer.valueOf(1).equals(item.getEnabled()), "物品未启用");

        ItemDetailVo vo = new ItemDetailVo();
        vo.setId(item.getId());
        vo.setCode(item.getCode());
        vo.setName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setMaxStack(item.getMaxStack());
        vo.setWeight(item.getWeight());
        List<String> codes = new ArrayList<>(GameItemTag.toCodes(item.getItemTags()));
        vo.setItemTagCodes(codes);
        vo.setTags(new ArrayList<>(GameItemTag.toLabels(item.getItemTags())));
        vo.setItemTag(codes.isEmpty() ? null : codes.get(0));
        if (ItemTagHelper.hasTag(item, GameItemTag.WEAPON)) {
            GameWeapon weapon = gameWeaponService.getByItemId(itemId);
            if (weapon != null) {
                vo.setWeaponAttack(weapon.getAttack());
                vo.setWeaponBaseActionValue(weapon.getBaseActionValue());
                vo.setWeaponDamageRatio(weapon.getDamageRatio());
            }
        }
        if (ItemTagHelper.hasTag(item, GameItemTag.ARMOR)) {
            GameArmor armor = gameArmorService.getByItemId(itemId);
            if (armor != null) {
                vo.setArmorBonusHp(armor.getBonusHp());
                vo.setArmorDefense(armor.getDefense());
            }
        }
        vo.setCrafts(listCraftPreviewsByMaterial(itemId));
        vo.setRemark(item.getRemark());
        return vo;
    }

    private List<ItemCraftPreviewVo> listCraftPreviewsByMaterial(String itemId) {
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
        Set<String> recipeIds = links.stream()
                .map(GameCraftMaterial::getRecipeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ItemCraftPreviewVo> previews = new ArrayList<>();
        for (String recipeId : recipeIds) {
            GameCraftRecipe recipe = craftRecipeMapper.selectById(recipeId);
            if (recipe == null || !Integer.valueOf(1).equals(recipe.getEnabled())) {
                continue;
            }
            GameItem result = this.getById(recipe.getResultItemId());
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
}
