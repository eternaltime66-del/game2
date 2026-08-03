package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameWeapon;
import org.wx.core.wxBusiness.game.entity.ItemFinishedSkillDetailVo;

import java.util.List;

@Service
public class BasicAttackService {

    public static final String DEFAULT_SKILL_ID = "fin_normal_attack";

    @Resource
    private GameWeaponService weaponService;
    @Resource
    private GameTriggerSlotService triggerSlotService;
    @Resource
    private GameItemService gameItemService;

    public String resolveMonsterSkillId(String monsterId) {
        String configured = triggerSlotService.findBasicAttackSkillIdByMonster(monsterId);
        return configured != null ? configured : DEFAULT_SKILL_ID;
    }

    public ItemFinishedSkillDetailVo resolveMonsterDetail(String monsterId) {
        return gameItemService.getFinishedSkillDetail(resolveMonsterSkillId(monsterId));
    }

    public String resolveSkillId(List<String> equippedItemIds) {
        String weaponSkillId = findWeaponBasicAttackSkillId(equippedItemIds);
        return weaponSkillId != null ? weaponSkillId : DEFAULT_SKILL_ID;
    }

    public String resolveSourceLabel(List<String> equippedItemIds) {
        if (equippedItemIds == null) {
            return "角色默认";
        }
        for (String itemId : equippedItemIds) {
            GameWeapon weapon = weaponService.getByItemId(itemId);
            if (weapon == null) {
                continue;
            }
            String skillId = triggerSlotService.findBasicAttackSkillId(List.of(itemId));
            if (skillId == null) {
                continue;
            }
            GameItem item = gameItemService.getById(itemId);
            return item != null ? "武器·" + item.getName() : "武器";
        }
        return "角色默认";
    }

    public ItemFinishedSkillDetailVo resolveDetail(List<String> equippedItemIds) {
        return gameItemService.getFinishedSkillDetail(resolveSkillId(equippedItemIds));
    }

    private String findWeaponBasicAttackSkillId(List<String> equippedItemIds) {
        if (equippedItemIds == null) {
            return null;
        }
        for (String itemId : equippedItemIds) {
            if (weaponService.getByItemId(itemId) == null) {
                continue;
            }
            String skillId = triggerSlotService.findBasicAttackSkillId(List.of(itemId));
            if (skillId != null) {
                return skillId;
            }
        }
        return null;
    }
}
