package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.entity.enums.HeroEquipSlot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HeroCombatService {

    @Resource
    private GameHeroEquipService heroEquipService;
    @Resource
    private GameWeaponService gameWeaponService;
    @Resource
    private GameArmorService gameArmorService;
    @Resource
    private HeroPassiveService heroPassiveService;
    @Resource
    private GameItemService gameItemService;

    public HeroCombatContext resolve(String uid, GameHero hero, List<GameBattleBag> bagRows, Map<String, GameItem> itemMap) {
        GameHeroEquip equip = heroEquipService.getOrInit(uid);
        return resolve(uid, hero, bagRows, itemMap, equip);
    }

    public HeroCombatContext resolve(String uid, GameHero hero, List<GameBattleBag> bagRows,
                                     Map<String, GameItem> itemMap, GameHeroEquip equip) {
        HeroCombatContext ctx = new HeroCombatContext();
        int unarmedAction = hero != null && hero.getActionValue() != null
                ? hero.getActionValue() : GameHero.DEFAULT_ACTION_VALUE;
        int heroAttack = hero != null && hero.getAttack() != null
                ? hero.getAttack() : GameHero.DEFAULT_ATTACK;
        int heroDefense = hero != null && hero.getDefense() != null
                ? hero.getDefense() : GameHero.DEFAULT_DEFENSE;
        int heroMaxHp = hero != null && hero.getMaxHp() != null
                ? hero.getMaxHp() : GameHero.DEFAULT_MAX_HP;
        BigDecimal ratio = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);

        ctx.setUnarmedActionValue(unarmedAction);
        ctx.setHeroAttack(heroAttack);
        ctx.setHeroDefense(heroDefense);
        ctx.setEquipAttack(0);
        ctx.setEquipDefense(0);
        ctx.setEquipBonusHp(0);
        ctx.setTotalAttack(heroAttack);
        ctx.setTotalDefense(heroDefense);
        ctx.setTotalMaxHp(heroMaxHp);
        ctx.setNormalAttackDamage(calcNormalAttackDamage(heroAttack, ratio));
        ctx.setBaseActionValue(unarmedAction);
        ctx.setDamageRatio(ratio);

        if (equip == null) {
            equip = heroEquipService.getOrInit(uid);
        }
        Set<String> equippedIds = new LinkedHashSet<>();
        Map<String, GameItem> equippedItems = new LinkedHashMap<>();

        for (HeroEquipSlot slot : HeroEquipSlot.values()) {
            String itemId = slot.getItemId(equip);
            if (itemId == null || itemId.isBlank()) {
                continue;
            }
            GameItem item = itemMap != null ? itemMap.get(itemId) : gameItemService.getById(itemId);
            if (item == null || !ItemTagHelper.hasTag(item, slot.getRequiredTag())) {
                continue;
            }
            equippedIds.add(itemId);
            equippedItems.put(itemId, item);
        }
        ctx.setEquippedItemIds(equippedIds);
        ctx.setEquippedItems(equippedItems);

        int equipDefense = 0;
        int equipBonusHp = 0;
        int equipBonusAttack = 0;
        for (GameItem equippedItem : equippedItems.values()) {
            GameArmor armor = gameArmorService.getByItemId(equippedItem.getId());
            if (armor == null) {
                continue;
            }
            equipDefense += armor.getDefense() != null ? armor.getDefense() : 0;
            equipBonusHp += armor.getBonusHp() != null ? armor.getBonusHp() : 0;
            equipBonusAttack += armor.getBonusAttack() != null ? armor.getBonusAttack() : 0;
        }
        ctx.setEquipDefense(equipDefense);
        ctx.setEquipBonusHp(equipBonusHp);
        ctx.setEquipAttack(equipBonusAttack);
        ctx.setTotalDefense(heroDefense + equipDefense);
        ctx.setTotalMaxHp(heroMaxHp + equipBonusHp);
        ctx.setTotalAttack(heroAttack + equipBonusAttack);
        ctx.setPassiveActionValueFactor(BigDecimal.ONE);

        String weaponItemId = equip.getWeaponItemId();
        if (weaponItemId != null && equippedIds.contains(weaponItemId)) {
            GameWeapon weapon = gameWeaponService.getByItemId(weaponItemId);
            GameItem weaponItem = equippedItems.get(weaponItemId);
            if (weapon != null && weaponItem != null && ItemTagHelper.hasTag(weaponItem, GameItemTag.WEAPON)) {
                ratio = weapon.getDamageRatio() != null ? weapon.getDamageRatio() : BigDecimal.ONE;
                int weaponAttack = weapon.getAttack() != null ? weapon.getAttack() : 0;
                int weaponBaseAction = weapon.getBaseActionValue() != null ? weapon.getBaseActionValue() : unarmedAction;
                int totalAttack = heroAttack + weaponAttack + equipBonusAttack;

                ctx.setEquipped(true);
                ctx.setEquippedWeaponItemId(weaponItemId);
                ctx.setEquippedWeaponItem(weaponItem);
                ctx.setEquippedWeapon(weapon);
                ctx.setEquipAttack(weaponAttack + equipBonusAttack);
                ctx.setTotalAttack(totalAttack);
                ctx.setBaseActionValue(weaponBaseAction);
                ctx.setDamageRatio(ratio.setScale(2, RoundingMode.HALF_UP));
                ctx.setEquippedWeaponVo(buildEquippedWeaponVo(weaponItem, weapon, heroAttack, totalAttack, ratio));
            }
        }

        heroPassiveService.applyPassives(ctx, equip, equippedItems);
        ctx.setNormalAttackDamage(calcNormalAttackDamage(ctx.getTotalAttack(), ctx.getDamageRatio()));
        if (ctx.getEquippedWeaponVo() != null) {
            ctx.getEquippedWeaponVo().setExpectDamage(BigDecimal.valueOf(ctx.getNormalAttackDamage())
                    .setScale(1, RoundingMode.HALF_UP));
        }
        return ctx;
    }

    /** 普攻伤害 = 总攻击 × 伤害比例 */
    public static int calcNormalAttackDamage(int totalAttack, BigDecimal damageRatio) {
        BigDecimal ratio = damageRatio != null ? damageRatio : BigDecimal.ONE;
        return BigDecimal.valueOf(totalAttack)
                .multiply(ratio)
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }

    private EquippedWeaponVo buildEquippedWeaponVo(GameItem item, GameWeapon weapon, int heroAttack,
                                                     int totalAttack, BigDecimal ratio) {
        EquippedWeaponVo vo = new EquippedWeaponVo();
        vo.setItemId(item.getId());
        vo.setItemCode(item.getCode());
        vo.setItemName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setAttack(weapon.getAttack());
        vo.setBaseActionValue(weapon.getBaseActionValue());
        BigDecimal damageRatio = ratio != null ? ratio : BigDecimal.ONE;
        vo.setDamageRatio(damageRatio.setScale(2, RoundingMode.HALF_UP));
        vo.setExpectDamage(BigDecimal.valueOf(calcNormalAttackDamage(totalAttack, damageRatio))
                .setScale(1, RoundingMode.HALF_UP));
        return vo;
    }

    public static class HeroCombatContext {
        private boolean equipped;
        private String equippedWeaponItemId;
        private GameItem equippedWeaponItem;
        private GameWeapon equippedWeapon;
        private EquippedWeaponVo equippedWeaponVo;
        private Set<String> equippedItemIds = new LinkedHashSet<>();
        private Map<String, GameItem> equippedItems = new LinkedHashMap<>();
        private int unarmedActionValue;
        private int heroAttack;
        private int heroDefense;
        private int equipAttack;
        private int equipDefense;
        private int equipBonusHp;
        private int totalAttack;
        private int totalDefense;
        private int totalMaxHp;
        private int normalAttackDamage;
        private int baseActionValue;
        private BigDecimal damageRatio;
        private BigDecimal passiveActionValueFactor = BigDecimal.ONE;

        public boolean isEquipped() { return equipped; }
        public void setEquipped(boolean equipped) { this.equipped = equipped; }
        public String getEquippedWeaponItemId() { return equippedWeaponItemId; }
        public void setEquippedWeaponItemId(String equippedWeaponItemId) { this.equippedWeaponItemId = equippedWeaponItemId; }
        public GameItem getEquippedWeaponItem() { return equippedWeaponItem; }
        public void setEquippedWeaponItem(GameItem equippedWeaponItem) { this.equippedWeaponItem = equippedWeaponItem; }
        public GameWeapon getEquippedWeapon() { return equippedWeapon; }
        public void setEquippedWeapon(GameWeapon equippedWeapon) { this.equippedWeapon = equippedWeapon; }
        public EquippedWeaponVo getEquippedWeaponVo() { return equippedWeaponVo; }
        public void setEquippedWeaponVo(EquippedWeaponVo equippedWeaponVo) { this.equippedWeaponVo = equippedWeaponVo; }
        public Set<String> getEquippedItemIds() { return equippedItemIds; }
        public void setEquippedItemIds(Set<String> equippedItemIds) { this.equippedItemIds = equippedItemIds; }
        public Map<String, GameItem> getEquippedItems() { return equippedItems; }
        public void setEquippedItems(Map<String, GameItem> equippedItems) { this.equippedItems = equippedItems; }
        public int getUnarmedActionValue() { return unarmedActionValue; }
        public void setUnarmedActionValue(int unarmedActionValue) { this.unarmedActionValue = unarmedActionValue; }
        public int getHeroAttack() { return heroAttack; }
        public void setHeroAttack(int heroAttack) { this.heroAttack = heroAttack; }
        public int getHeroDefense() { return heroDefense; }
        public void setHeroDefense(int heroDefense) { this.heroDefense = heroDefense; }
        public int getEquipAttack() { return equipAttack; }
        public void setEquipAttack(int equipAttack) { this.equipAttack = equipAttack; }
        public int getEquipDefense() { return equipDefense; }
        public void setEquipDefense(int equipDefense) { this.equipDefense = equipDefense; }
        public int getEquipBonusHp() { return equipBonusHp; }
        public void setEquipBonusHp(int equipBonusHp) { this.equipBonusHp = equipBonusHp; }
        public int getTotalAttack() { return totalAttack; }
        public void setTotalAttack(int totalAttack) { this.totalAttack = totalAttack; }
        public int getTotalDefense() { return totalDefense; }
        public void setTotalDefense(int totalDefense) { this.totalDefense = totalDefense; }
        public int getTotalMaxHp() { return totalMaxHp; }
        public void setTotalMaxHp(int totalMaxHp) { this.totalMaxHp = totalMaxHp; }
        public int getNormalAttackDamage() { return normalAttackDamage; }
        public void setNormalAttackDamage(int normalAttackDamage) { this.normalAttackDamage = normalAttackDamage; }
        public int getBaseActionValue() { return baseActionValue; }
        public void setBaseActionValue(int baseActionValue) { this.baseActionValue = baseActionValue; }
        public BigDecimal getDamageRatio() { return damageRatio; }
        public void setDamageRatio(BigDecimal damageRatio) { this.damageRatio = damageRatio; }
        public BigDecimal getPassiveActionValueFactor() { return passiveActionValueFactor; }
        public void setPassiveActionValueFactor(BigDecimal passiveActionValueFactor) {
            this.passiveActionValueFactor = passiveActionValueFactor != null ? passiveActionValueFactor : BigDecimal.ONE;
        }
    }
}
