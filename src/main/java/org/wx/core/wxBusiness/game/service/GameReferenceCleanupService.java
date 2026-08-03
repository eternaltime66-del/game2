package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.mapper.GameRecipeMaterialMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 删除主实体时，清理关联绑定数据 */
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

    /** 删除物品前：扳机槽 / 特性被动 / 配方引用 */
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

    /** 删除被动技能前：装备/怪物被动绑定、技能徽章 */
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
            skillBadgeService.removeById(badge.getItemId());
            itemService.removeById(badge.getItemId());
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
