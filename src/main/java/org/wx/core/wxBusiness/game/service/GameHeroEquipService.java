package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.entity.enums.HeroEquipSlot;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.wx.core.wxBusiness.game.mapper.GameHeroEquipMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GameHeroEquipService extends WxServiceImpl<GameHeroEquipMapper, GameHeroEquip> {

    @Resource
    private GameBattleBagService battleBagService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameWeaponService gameWeaponService;
    @Resource
    private GameArmorService gameArmorService;
    @Resource
    private GameInventoryService inventoryService;

    public GameHeroEquip getOrInit(String uid) {
        GameHeroEquip row = this.find().eq(GameHeroEquip::getUid, uid).one();
        if (row != null) {
            return row;
        }
        row = GameHeroEquip.empty(uid);
        this.save(row);
        return row;
    }

    @Transactional(rollbackFor = Exception.class)
    public GameHeroEquip equipSlot(String uid, HeroEquipSlot slot, String itemId) {
        ErrorFactory.notNull(slot, "请选择装备槽位");
        ErrorFactory.notNull(itemId, "请选择装备");
        GameItem item = gameItemService.getById(itemId);
        ErrorFactory.notNull(item, "物品不存在");
        ErrorFactory.throwError(!ItemTagHelper.hasTag(item, slot.getRequiredTag()),
                "该物品不能装备到" + slot.getLabel());
        if (slot == HeroEquipSlot.WEAPON) {
            ErrorFactory.notNull(gameWeaponService.getByItemId(itemId), "武器配置不存在");
        }
        if (slot == HeroEquipSlot.ARMOR) {
            ErrorFactory.notNull(gameArmorService.getByItemId(itemId), "护甲配置不存在");
        }

        GameHeroEquip equip = getOrInit(uid);
        assertNotEquippedElsewhere(equip, slot, itemId);

        String currentItemId = slot.getItemId(equip);
        if (itemId.equals(currentItemId)) {
            return equip;
        }

        if (currentItemId != null && !currentItemId.isBlank()) {
            unequipSlotInternal(uid, equip, slot);
        }

        int beforeQty = battleBagService.consumeQuantity(uid, itemId, 1);
        inventoryService.saveItemLog(uid, itemId, item.getName(), -1, beforeQty, beforeQty - 1,
                GameItemLog.REASON_EQUIP, slot.name(), "装备到" + slot.getLabel());

        slot.setItemId(equip, itemId);
        persistSlotItemId(equip, slot, itemId);
        if (slot == HeroEquipSlot.WEAPON) {
            GameWeapon weapon = gameWeaponService.getByItemId(itemId);
            Integer uses = null;
            if (weapon != null && weapon.getConsumable() != null && weapon.getConsumable() == 1) {
                uses = weapon.getMaxUses() != null && weapon.getMaxUses() > 0 ? weapon.getMaxUses() : 1;
            }
            equip.setWeaponUsesLeft(uses);
            this.update(updateWrapper()
                    .eq(GameHeroEquip::getId, equip.getId())
                    .set(GameHeroEquip::getWeaponUsesLeft, uses));
        }
        return equip;
    }

    @Transactional(rollbackFor = Exception.class)
    public GameHeroEquip unequipSlot(String uid, HeroEquipSlot slot) {
        ErrorFactory.notNull(slot, "请选择装备槽位");
        GameHeroEquip equip = getOrInit(uid);
        unequipSlotInternal(uid, equip, slot);
        persistSlotItemId(equip, slot, null);
        return equip;
    }

    private void unequipSlotInternal(String uid, GameHeroEquip equip, HeroEquipSlot slot) {
        String itemId = slot.getItemId(equip);
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        GameItem item = gameItemService.getById(itemId);
        String itemName = item != null ? item.getName() : itemId;

        slot.setItemId(equip, null);
        if (slot == HeroEquipSlot.WEAPON) {
            equip.setWeaponUsesLeft(null);
            this.update(updateWrapper()
                    .eq(GameHeroEquip::getId, equip.getId())
                    .set(GameHeroEquip::getWeaponUsesLeft, null));
        }
        int beforeQty = battleBagService.grantQuantity(uid, itemId, 1);
        inventoryService.saveItemLog(uid, itemId, itemName, 1, beforeQty, beforeQty + 1,
                GameItemLog.REASON_UNEQUIP, slot.name(), "从" + slot.getLabel() + "卸下");
    }

    /** MyBatis-Plus 默认跳过 null 字段，卸下装备需显式写入 null */
    private void persistSlotItemId(GameHeroEquip equip, HeroEquipSlot slot, String itemId) {
        slot.setItemId(equip, itemId);
        LambdaUpdateWrapper<GameHeroEquip> wrapper = updateWrapper()
                .eq(GameHeroEquip::getId, equip.getId());
        switch (slot) {
            case WEAPON -> wrapper.set(GameHeroEquip::getWeaponItemId, itemId);
            case ARMOR -> wrapper.set(GameHeroEquip::getArmorItemId, itemId);
            case GLOVES -> wrapper.set(GameHeroEquip::getGlovesItemId, itemId);
            case LEGS -> wrapper.set(GameHeroEquip::getLegsItemId, itemId);
            case HELMET -> wrapper.set(GameHeroEquip::getHelmetItemId, itemId);
            case ACCESSORY_1 -> wrapper.set(GameHeroEquip::getAccessory1ItemId, itemId);
            case ACCESSORY_2 -> wrapper.set(GameHeroEquip::getAccessory2ItemId, itemId);
            case ACCESSORY_3 -> wrapper.set(GameHeroEquip::getAccessory3ItemId, itemId);
            case SKILL_1 -> wrapper.set(GameHeroEquip::getSkillBadge1ItemId, itemId);
            case SKILL_2 -> wrapper.set(GameHeroEquip::getSkillBadge2ItemId, itemId);
            case SKILL_3 -> wrapper.set(GameHeroEquip::getSkillBadge3ItemId, itemId);
            case SKILL_4 -> wrapper.set(GameHeroEquip::getSkillBadge4ItemId, itemId);
        }
        this.update(wrapper);
    }

    private void assertNotEquippedElsewhere(GameHeroEquip equip, HeroEquipSlot targetSlot, String itemId) {
        for (HeroEquipSlot slot : HeroEquipSlot.values()) {
            if (slot == targetSlot) {
                continue;
            }
            String equippedId = slot.getItemId(equip);
            if (itemId.equals(equippedId)) {
                ErrorFactory.throwError(true, "该物品已在" + slot.getLabel() + "装备");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GameHeroEquip equipWeapon(String uid, String itemId) {
        return equipSlot(uid, HeroEquipSlot.WEAPON, itemId);
    }

    @Transactional(rollbackFor = Exception.class)
    public GameHeroEquip unequipWeapon(String uid) {
        return unequipSlot(uid, HeroEquipSlot.WEAPON);
    }

    public List<HeroEquipSlotVo> buildSlotOverview(GameHeroEquip equip, Map<String, GameItem> itemMap,
                                                   HeroCombatService.HeroCombatContext combat) {
        List<HeroEquipSlotVo> slots = new ArrayList<>();
        for (HeroEquipSlot slot : HeroEquipSlot.values()) {
            HeroEquipSlotVo vo = new HeroEquipSlotVo();
            vo.setSlot(slot.name());
            vo.setLabel(slot.getLabel());
            String itemId = slot.getItemId(equip);
            if (itemId == null || itemId.isBlank()) {
                slots.add(vo);
                continue;
            }
            GameItem item = itemMap != null ? itemMap.get(itemId) : null;
            if (item == null) {
                slots.add(vo);
                continue;
            }
            vo.setItem(buildEquippedItemVo(item));
            if (slot == HeroEquipSlot.WEAPON && combat != null && combat.getEquippedWeaponVo() != null
                    && itemId.equals(combat.getEquippedWeaponItemId())) {
                EquippedWeaponVo weaponVo = combat.getEquippedWeaponVo();
                vo.setWeaponAttack(weaponVo.getAttack());
                vo.setWeaponBaseActionValue(weaponVo.getBaseActionValue());
                vo.setWeaponDamageRatio(weaponVo.getDamageRatio());
            }
            slots.add(vo);
        }
        return slots;
    }

    public static EquippedItemVo buildEquippedItemVo(GameItem item) {
        EquippedItemVo vo = new EquippedItemVo();
        vo.setItemId(item.getId());
        vo.setItemCode(item.getCode());
        vo.setItemName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setTags(new ArrayList<>(GameItemTag.toLabels(item.getItemTags())));
        return vo;
    }
}
