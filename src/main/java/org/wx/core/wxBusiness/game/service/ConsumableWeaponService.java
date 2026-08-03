package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBusiness.game.entity.BattleLog;
import org.wx.core.wxBusiness.game.entity.BattleState;
import org.wx.core.wxBusiness.game.entity.BattleUnit;
import org.wx.core.wxBusiness.game.entity.GameHeroEquip;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameWeapon;
import org.wx.core.wxBusiness.game.entity.enums.HeroEquipSlot;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.util.ArrayList;
import java.util.List;

/** 消耗型武器：战斗中扣次数，归零脱手并回退空手普攻 */
@Service
public class ConsumableWeaponService {

    @Resource
    private GameHeroEquipService heroEquipService;
    @Resource
    private GameWeaponService weaponService;
    @Resource
    private GameItemService itemService;
    @Lazy
    @Resource
    private GamePrepService gamePrepService;

    public void initBattleState(BattleState state) {
        GameHeroEquip equip = heroEquipService.getOrInit(state.getUid());
        String weaponItemId = equip.getWeaponItemId();
        if (weaponItemId == null || weaponItemId.isBlank()) {
            state.setConsumableWeaponItemId(null);
            state.setWeaponUsesLeft(null);
            return;
        }
        GameWeapon weapon = weaponService.getByItemId(weaponItemId);
        if (weapon == null || weapon.getConsumable() == null || weapon.getConsumable() != 1) {
            state.setConsumableWeaponItemId(null);
            state.setWeaponUsesLeft(null);
            return;
        }
        state.setConsumableWeaponItemId(weaponItemId);
        Integer left = equip.getWeaponUsesLeft();
        if (left == null) {
            left = weapon.getMaxUses() != null ? weapon.getMaxUses() : 1;
            persistUsesLeft(equip.getId(), left);
        }
        state.setWeaponUsesLeft(left);
    }

    /**
     * 若本次技能来自消耗型武器（或空手前的武器普攻来源），扣 1 次；归零则脱手。
     * @return 追加战斗日志（可能为空）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<BattleLog> afterWeaponSkillUse(BattleState state, String sourceItemId) {
        if (state.getConsumableWeaponItemId() == null || state.getWeaponUsesLeft() == null) {
            return List.of();
        }
        if (sourceItemId == null || !sourceItemId.equals(state.getConsumableWeaponItemId())) {
            return List.of();
        }
        int left = state.getWeaponUsesLeft() - 1;
        state.setWeaponUsesLeft(Math.max(left, 0));
        GameHeroEquip equip = heroEquipService.getOrInit(state.getUid());
        persistUsesLeft(equip.getId(), state.getWeaponUsesLeft());

        if (left > 0) {
            return List.of();
        }
        return autoUnequip(state, equip);
    }

    /** 行动值满普攻：若当前普攻来自消耗型武器则扣次 */
    @Transactional(rollbackFor = Exception.class)
    public List<BattleLog> afterBasicAttack(BattleState state) {
        String weaponItemId = state.getConsumableWeaponItemId();
        if (weaponItemId == null) {
            return List.of();
        }
        return afterWeaponSkillUse(state, weaponItemId);
    }

    private List<BattleLog> autoUnequip(BattleState state, GameHeroEquip equip) {
        String weaponItemId = state.getConsumableWeaponItemId();
        GameItem item = weaponItemId != null ? itemService.getById(weaponItemId) : null;
        String name = item != null ? item.getName() : "武器";

        List<String> equipped = state.getHeroEquippedItemIds();
        if (equipped != null) {
            List<String> next = new ArrayList<>(equipped);
            next.remove(weaponItemId);
            state.setHeroEquippedItemIds(next);
        }
        state.setConsumableWeaponItemId(null);
        state.setWeaponUsesLeft(null);

        if (equip.getWeaponItemId() != null && equip.getWeaponItemId().equals(weaponItemId)) {
            heroEquipService.unequipSlot(state.getUid(), HeroEquipSlot.WEAPON);
            persistUsesLeft(equip.getId(), null);
        }

        BattleUnit hero = state.getUnits().stream()
                .filter(u -> BattleUnit.SIDE_HERO.equals(u.getSide()))
                .findFirst()
                .orElse(null);
        if (hero != null) {
            hero.setAttack(gamePrepService.resolveBattleTotalAttack(state.getUid()));
            hero.setDefense(gamePrepService.resolveBattleDefense(state.getUid()));
            hero.setActionValue(gamePrepService.resolveBattleActionValue(state.getUid()));
            hero.setWeaponDamageRatio(null);
        }

        return List.of(BattleLog.of(BattleLog.TYPE_SKILL, name + " 使用次数耗尽，已自动脱手，改为空手普攻"));
    }

    private void persistUsesLeft(String equipId, Integer usesLeft) {
        LambdaUpdateWrapper<GameHeroEquip> wrapper = heroEquipService.updateWrapper()
                .eq(GameHeroEquip::getId, equipId)
                .set(GameHeroEquip::getWeaponUsesLeft, usesLeft);
        heroEquipService.update(wrapper);
    }
}
