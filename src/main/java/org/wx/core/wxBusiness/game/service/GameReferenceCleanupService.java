package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.HeroEquipSlot;
import org.wx.core.wxBusiness.game.mapper.GameRecipeMaterialMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 删除主实体时，清理关联绑定数据与玩家持有物 */
@Service
public class GameReferenceCleanupService {

    @Resource
    private GameTriggerSlotService triggerSlotService;
    @Resource
    private GameCompleteSkillService completeSkillService;
    @Resource
    private GameItemPassiveService itemPassiveService;
    @Resource
    private GameMonsterPassiveService monsterPassiveService;
    @Resource
    private GameRecipeService recipeService;
    @Resource
    private GameRecipeMaterialMapper recipeMaterialMapper;
    @Resource
    private GameSkillBadgeService skillBadgeService;
    @Resource
    private GameItemService itemService;
    @Resource
    private GameFinishedSkillService finishedSkillService;
    @Resource
    private GameInventoryService inventoryService;
    @Resource
    private GameBattleBagService battleBagService;
    @Resource
    private GameHeroEquipService heroEquipService;
    @Resource
    private GameProfessionSkillService professionSkillService;
    @Resource
    private GameMonsterDropService monsterDropService;
    @Resource
    private GameStageDropService stageDropService;

    /** 删除成品技能前：扳机槽 / 完整技能组中引用该技能的绑定 */
    @Transactional(rollbackFor = Exception.class)
    public void removeFinishedSkillBindings(String finishedSkillId) {
        if (finishedSkillId == null || finishedSkillId.isBlank()) {
            return;
        }
        triggerSlotService.remove(new LambdaQueryWrapper<GameTriggerSlot>()
                .eq(GameTriggerSlot::getFinishedSkillId, finishedSkillId)
                .or(w -> w.eq(GameTriggerSlot::getTriggerRefId, finishedSkillId)));
        completeSkillService.remove(new LambdaQueryWrapper<GameCompleteSkill>()
                .eq(GameCompleteSkill::getFinishedSkillId, finishedSkillId)
                .or(w -> w.eq(GameCompleteSkill::getTriggerRefId, finishedSkillId)));
    }

    /** 删除物品前：绑定关系 + 玩家仓库/背包/装备槽 + 掉落/职业技能引用 */
    @Transactional(rollbackFor = Exception.class)
    public void removeItemBindings(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        triggerSlotService.remove(new LambdaQueryWrapper<GameTriggerSlot>()
                .eq(GameTriggerSlot::getItemId, itemId));
        itemPassiveService.remove(new LambdaQueryWrapper<GameItemPassive>()
                .eq(GameItemPassive::getItemId, itemId));

        List<GameRecipe> outputRecipes = recipeService.find()
                .eq(GameRecipe::getOutputItemId, itemId)
                .list();
        for (GameRecipe recipe : outputRecipes) {
            recipeMaterialMapper.delete(new LambdaQueryWrapper<GameRecipeMaterial>()
                    .eq(GameRecipeMaterial::getRecipeId, recipe.getId()));
            recipeService.removeById(recipe.getId());
        }
        recipeMaterialMapper.delete(new LambdaQueryWrapper<GameRecipeMaterial>()
                .eq(GameRecipeMaterial::getMaterialItemId, itemId));

        professionSkillService.remove(new LambdaQueryWrapper<GameProfessionSkill>()
                .eq(GameProfessionSkill::getItemId, itemId));
        monsterDropService.remove(new LambdaQueryWrapper<GameMonsterDrop>()
                .eq(GameMonsterDrop::getItemId, itemId));
        stageDropService.remove(new LambdaQueryWrapper<GameStageDrop>()
                .eq(GameStageDrop::getItemId, itemId));

        GameSkillBadge badge = skillBadgeService.getByItemId(itemId);
        if (badge != null) {
            skillBadgeService.removeById(badge.getItemId());
        }

        removePlayerItemHoldings(itemId);
    }

    /** 删除玩家持有：仓库、战斗背包、角色装备槽 */
    @Transactional(rollbackFor = Exception.class)
    public void removePlayerItemHoldings(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        inventoryService.remove(new LambdaQueryWrapper<GameInventory>()
                .eq(GameInventory::getItemId, itemId));
        battleBagService.remove(new LambdaQueryWrapper<GameBattleBag>()
                .eq(GameBattleBag::getItemId, itemId));

        List<GameHeroEquip> equips = heroEquipService.list();
        for (GameHeroEquip equip : equips) {
            boolean changed = false;
            for (HeroEquipSlot slot : HeroEquipSlot.values()) {
                String equipped = slot.getItemId(equip);
                if (itemId.equals(equipped)) {
                    slot.setItemId(equip, null);
                    if (slot == HeroEquipSlot.WEAPON) {
                        equip.setWeaponUsesLeft(null);
                    }
                    changed = true;
                }
            }
            if (itemId.equals(equip.getSkillBadge1ItemId())) {
                equip.setSkillBadge1ItemId(null);
                changed = true;
            }
            if (itemId.equals(equip.getSkillBadge2ItemId())) {
                equip.setSkillBadge2ItemId(null);
                changed = true;
            }
            if (itemId.equals(equip.getSkillBadge3ItemId())) {
                equip.setSkillBadge3ItemId(null);
                changed = true;
            }
            if (itemId.equals(equip.getSkillBadge4ItemId())) {
                equip.setSkillBadge4ItemId(null);
                changed = true;
            }
            if (changed) {
                heroEquipService.updateById(equip);
            }
        }
    }

    /** 删除怪物前：扳机槽 / 特性被动 */
    @Transactional(rollbackFor = Exception.class)
    public void removeMonsterBindings(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return;
        }
        triggerSlotService.remove(new LambdaQueryWrapper<GameTriggerSlot>()
                .eq(GameTriggerSlot::getMonsterId, monsterId));
        monsterPassiveService.remove(new LambdaQueryWrapper<GameMonsterPassive>()
                .eq(GameMonsterPassive::getMonsterId, monsterId));
    }

    /** 删除被动技能前：装备/怪物被动绑定、技能徽章（含玩家持有） */
    @Transactional(rollbackFor = Exception.class)
    public void removePassiveSkillBindings(String passiveSkillId) {
        if (passiveSkillId == null || passiveSkillId.isBlank()) {
            return;
        }
        itemPassiveService.remove(new LambdaQueryWrapper<GameItemPassive>()
                .eq(GameItemPassive::getPassiveSkillId, passiveSkillId));
        monsterPassiveService.remove(new LambdaQueryWrapper<GameMonsterPassive>()
                .eq(GameMonsterPassive::getPassiveSkillId, passiveSkillId));

        List<GameSkillBadge> badges = skillBadgeService.find()
                .eq(GameSkillBadge::getPassiveSkillId, passiveSkillId)
                .list();
        for (GameSkillBadge badge : badges) {
            if (badge.getItemId() == null || badge.getItemId().isBlank()) {
                continue;
            }
            String badgeItemId = badge.getItemId();
            removeItemBindings(badgeItemId);
            itemService.removeById(badgeItemId);
        }
    }

    /** 清理已不存在成品技能上的孤儿绑定（维护用） */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupOrphanFinishedSkillBindings() {
        Set<String> validSkillIds = finishedSkillService.list().stream()
                .map(GameFinishedSkill::getId)
                .collect(Collectors.toCollection(HashSet::new));
        int removed = 0;
        for (GameTriggerSlot slot : triggerSlotService.list()) {
            if (isBrokenSkillRef(slot.getFinishedSkillId(), validSkillIds)
                    || isBrokenSkillRef(slot.getTriggerRefId(), validSkillIds)) {
                triggerSlotService.removeById(slot.getId());
                removed++;
            }
        }
        for (GameCompleteSkill skill : completeSkillService.list()) {
            if (isBrokenSkillRef(skill.getFinishedSkillId(), validSkillIds)
                    || isBrokenSkillRef(skill.getTriggerRefId(), validSkillIds)) {
                completeSkillService.removeById(skill.getId());
                removed++;
            }
        }
        return removed;
    }

    private boolean isBrokenSkillRef(String skillId, Set<String> validSkillIds) {
        if (skillId == null || skillId.isBlank()) {
            return false;
        }
        return !validSkillIds.contains(skillId);
    }
}
