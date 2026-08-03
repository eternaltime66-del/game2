package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.factory.PageFactory;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台分类物品管理：材料 / 武器 / 防具
 * 武器、防具：先写扩展表，再同步 app_game_item
 */
@Service
public class GameItemCategoryAdminService {

    @Resource
    private GameItemService itemService;
    @Resource
    private GameWeaponService weaponService;
    @Resource
    private GameArmorService armorService;
    @Resource
    private GameInventoryService inventoryService;
    @Resource
    private GameReferenceCleanupService referenceCleanupService;

    public IPage<AdminMaterialVo> listMaterials(GameItem query) {
        List<GameItem> all = itemService.find().list().stream()
                .filter(item -> ItemTagHelper.isPureMaterial(item.getItemTags()))
                .collect(Collectors.toList());
        return sliceVo(all.stream().map(this::toMaterialVo).collect(Collectors.toList()), query);
    }

    public IPage<AdminWeaponVo> listWeapons(GameWeapon query) {
        List<GameWeapon> weapons = weaponService.find().orderByAsc(GameWeapon::getId).list();
        List<AdminWeaponVo> vos = new ArrayList<>();
        for (GameWeapon weapon : weapons) {
            GameItem item = itemService.getById(weapon.getItemId());
            if (item == null) {
                continue;
            }
            vos.add(toWeaponVo(weapon, item));
        }
        return sliceVo(vos, query);
    }

    public IPage<AdminArmorVo> listArmors(GameArmor query) {
        return listArmorLike(GameItemTag.ARMOR, query);
    }

    public IPage<AdminArmorVo> listGloves(GameArmor query) {
        return listArmorLike(GameItemTag.GLOVES, query);
    }

    public IPage<AdminArmorVo> listHelmets(GameArmor query) {
        return listArmorLike(GameItemTag.HELMET, query);
    }

    public IPage<AdminArmorVo> listLegs(GameArmor query) {
        return listArmorLike(GameItemTag.LEGS, query);
    }

    public IPage<AdminArmorVo> listAccessories(GameArmor query) {
        return listArmorLike(GameItemTag.ACCESSORY, query);
    }

    private IPage<AdminArmorVo> listArmorLike(GameItemTag tag, GameArmor query) {
        List<AdminArmorVo> vos = new ArrayList<>();
        for (GameArmor armor : armorService.find().orderByAsc(GameArmor::getId).list()) {
            GameItem item = itemService.getById(armor.getItemId());
            if (item == null || !ItemTagHelper.hasTag(item, tag)) {
                continue;
            }
            vos.add(toArmorVo(armor, item));
        }
        return sliceVo(vos, query);
    }

    public List<GameItem> listAllItemsForPicker() {
        return itemService.find().orderByAsc(GameItem::getSort).list();
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminMaterialVo saveMaterial(AdminMaterialVo vo) {
        ErrorFactory.notNull(vo.getCode(), "编码不能为空");
        ErrorFactory.notNull(vo.getName(), "名称不能为空");

        GameItem item = new GameItem();
        item.setId(vo.getId());
        item.setCode(vo.getCode().trim().toUpperCase());
        item.setName(vo.getName().trim());
        item.setIcon(vo.getIcon() != null ? vo.getIcon() : "📦");
        item.setItemTags(GameItemTag.MATERIAL.name());
        item.setMaxStack(vo.getMaxStack() != null ? vo.getMaxStack() : 99);
        item.setSort(vo.getSort() != null ? vo.getSort() : 0);
        item.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        item.setRemark(vo.getRemark());

        if (item.getId() == null || item.getId().isBlank()) {
            item.setId("item_" + item.getCode().toLowerCase());
            itemService.save(item);
        } else {
            GameItem exists = itemService.getById(item.getId());
            ErrorFactory.notNull(exists, "材料不存在");
            ErrorFactory.throwError(!ItemTagHelper.isPureMaterial(exists.getItemTags()), "该物品不是纯材料，请在对应分类管理");
            Integer oldMaxStack = exists.getMaxStack();
            itemService.updateById(item);
            restackIfMaxStackChanged(item.getId(), oldMaxStack, item.getMaxStack());
        }
        return toMaterialVo(itemService.getById(item.getId()));
    }

    private void restackIfMaxStackChanged(String itemId, Integer oldMaxStack, Integer newMaxStack) {
        if (oldMaxStack == null || newMaxStack == null || oldMaxStack.equals(newMaxStack)) {
            return;
        }
        inventoryService.restackItemInAllWarehouses(itemId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMaterial(String itemId) {
        ErrorFactory.notNull(itemId, "ID不能为空");
        GameItem item = itemService.getById(itemId);
        ErrorFactory.notNull(item, "材料不存在");
        ErrorFactory.throwError(!ItemTagHelper.isPureMaterial(item.getItemTags()), "该物品不是纯材料");
        referenceCleanupService.removeItemBindings(itemId);
        itemService.removeById(itemId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminWeaponVo saveWeapon(AdminWeaponVo vo) {
        ErrorFactory.notNull(vo.getCode(), "编码不能为空");
        ErrorFactory.notNull(vo.getName(), "名称不能为空");
        ErrorFactory.notNull(vo.getAttack(), "攻击力不能为空");

        boolean isNew = vo.getId() == null || vo.getId().isBlank();
        String itemId = vo.getItemId();
        if (!isNew) {
            GameWeapon exists = weaponService.getById(vo.getId());
            ErrorFactory.notNull(exists, "武器不存在");
            itemId = exists.getItemId();
        } else {
            itemId = generateUniqueItemId();
        }

        GameWeapon weapon = new GameWeapon();
        weapon.setItemId(itemId);
        weapon.setAttack(vo.getAttack());
        weapon.setBaseActionValue(vo.getBaseActionValue() != null ? vo.getBaseActionValue() : 100);
        weapon.setDamageRatio(vo.getDamageRatio() != null ? vo.getDamageRatio() : BigDecimal.ONE);
        int consumable = vo.getConsumable() != null && vo.getConsumable() == 1 ? 1 : 0;
        weapon.setConsumable(consumable);
        if (consumable == 1) {
            ErrorFactory.throwError(vo.getMaxUses() == null || vo.getMaxUses() < 1, "消耗型武器请填写最大使用次数");
            weapon.setMaxUses(vo.getMaxUses());
        } else {
            weapon.setMaxUses(null);
        }
        weapon.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        weapon.setRemark(vo.getRemark());

        if (isNew) {
            weapon.setId(generateUniqueWeaponId());
            weaponService.save(weapon);
        } else {
            weapon.setId(vo.getId());
            weaponService.updateById(weapon);
        }

        syncItemFromWeapon(vo, itemId);
        GameItem item = itemService.getById(itemId);
        return toWeaponVo(weaponService.getById(weapon.getId()), item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeWeapon(String weaponId) {
        ErrorFactory.notNull(weaponId, "武器ID不能为空");
        GameWeapon weapon = weaponService.getById(weaponId);
        ErrorFactory.notNull(weapon, "武器不存在");
        String itemId = weapon.getItemId();
        weaponService.removeById(weaponId);
        if (itemId != null && !itemId.isBlank()) {
            referenceCleanupService.removeItemBindings(itemId);
            itemService.removeById(itemId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminArmorVo saveArmor(AdminArmorVo vo) {
        return saveArmorLike(vo, GameItemTag.ARMOR);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminArmorVo saveGloves(AdminArmorVo vo) {
        return saveArmorLike(vo, GameItemTag.GLOVES);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminArmorVo saveHelmet(AdminArmorVo vo) {
        return saveArmorLike(vo, GameItemTag.HELMET);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminArmorVo saveLegs(AdminArmorVo vo) {
        return saveArmorLike(vo, GameItemTag.LEGS);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminArmorVo saveAccessory(AdminArmorVo vo) {
        return saveArmorLike(vo, GameItemTag.ACCESSORY);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminArmorVo saveArmorLike(AdminArmorVo vo, GameItemTag tag) {
        ErrorFactory.notNull(tag, "装备类型不能为空");
        ErrorFactory.notNull(vo.getName(), "名称不能为空");

        boolean isNew = vo.getId() == null || vo.getId().isBlank();
        String code = vo.getCode() != null ? vo.getCode().trim() : "";
        if (code.isEmpty()) {
            if (isNew) {
                code = generateUniqueEquipCode(tag);
            } else {
                GameArmor exists = armorService.getById(vo.getId());
                ErrorFactory.notNull(exists, equipTypeLabel(tag) + "不存在");
                GameItem linked = itemService.getById(exists.getItemId());
                ErrorFactory.notNull(linked, "关联物品不存在");
                code = linked.getCode() != null ? linked.getCode().trim() : generateUniqueEquipCode(tag);
            }
        }
        vo.setCode(code);

        String itemId = vo.getItemId();
        if (!isNew) {
            GameArmor exists = armorService.getById(vo.getId());
            ErrorFactory.notNull(exists, equipTypeLabel(tag) + "不存在");
            itemId = itemId != null && !itemId.isBlank() ? itemId : exists.getItemId();
            GameItem item = itemService.getById(itemId);
            ErrorFactory.notNull(item, "关联物品不存在");
            ErrorFactory.throwError(!ItemTagHelper.hasTag(item, tag),
                    "该记录不是" + equipTypeLabel(tag) + "，请在对应分类管理");
        } else {
            if (itemId == null || itemId.isBlank()) {
                itemId = generateUniqueItemId();
            }
        }

        GameArmor armor = new GameArmor();
        armor.setItemId(itemId);
        armor.setBonusHp(vo.getBonusHp() != null ? vo.getBonusHp() : 0);
        armor.setDefense(vo.getDefense() != null ? vo.getDefense() : 0);
        armor.setBonusAttack(vo.getBonusAttack() != null ? vo.getBonusAttack() : 0);
        armor.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        armor.setRemark(vo.getRemark());

        if (isNew) {
            armor.setId(generateUniqueArmorId(tag, code));
            armorService.save(armor);
        } else {
            armor.setId(vo.getId());
            armorService.updateById(armor);
        }

        syncItemFromArmorLike(vo, itemId, tag);
        GameItem item = itemService.getById(itemId);
        return toArmorVo(armorService.getById(armor.getId()), item);
    }

    private String generateUniqueEquipCode(GameItemTag tag) {
        String prefix = switch (tag) {
            case ACCESSORY -> "ACC";
            case GLOVES -> "GLOVES";
            case HELMET -> "HELMET";
            case LEGS -> "LEGS";
            default -> "ARMOR";
        };
        String code;
        do {
            code = prefix + "_" + WordUnit.randomKey(6, 3).toUpperCase();
        } while (itemService.find().eq(GameItem::getCode, code).one() != null);
        return code;
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeArmor(String armorId) {
        ErrorFactory.notNull(armorId, "防具ID不能为空");
        GameArmor armor = armorService.getById(armorId);
        ErrorFactory.notNull(armor, "防具不存在");
        String itemId = armor.getItemId();
        armorService.removeById(armorId);
        if (itemId != null && !itemId.isBlank()) {
            referenceCleanupService.removeItemBindings(itemId);
            itemService.removeById(itemId);
        }
    }

    private void syncItemFromWeapon(AdminWeaponVo vo, String itemId) {
        GameItem item = itemService.getById(itemId);
        boolean isNew = item == null;
        if (isNew) {
            item = new GameItem();
            item.setId(itemId);
        }
        item.setCode(vo.getCode().trim().toUpperCase());
        item.setName(vo.getName().trim());
        item.setIcon(vo.getIcon() != null ? vo.getIcon() : "⚔");
        item.setItemTags(GameItemTag.WEAPON.name());
        item.setMaxStack(1);
        item.setSort(vo.getSort() != null ? vo.getSort() : 0);
        item.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        item.setRemark(vo.getRemark());
        if (isNew) {
            itemService.save(item);
        } else {
            itemService.updateById(item);
        }
    }

    private void syncItemFromArmor(AdminArmorVo vo, String itemId) {
        syncItemFromArmorLike(vo, itemId, GameItemTag.ARMOR);
    }

    private void syncItemFromArmorLike(AdminArmorVo vo, String itemId, GameItemTag tag) {
        GameItem item = itemService.getById(itemId);
        boolean isNew = item == null;
        if (isNew) {
            item = new GameItem();
            item.setId(itemId);
        }
        item.setCode(vo.getCode().trim().toUpperCase());
        item.setName(vo.getName().trim());
        item.setIcon(vo.getIcon() != null ? vo.getIcon() : defaultEquipIcon(tag));
        item.setItemTags(tag.name());
        item.setMaxStack(1);
        item.setSort(vo.getSort() != null ? vo.getSort() : 0);
        item.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        item.setRemark(vo.getRemark());
        if (isNew) {
            itemService.save(item);
        } else {
            itemService.updateById(item);
        }
    }

    private String defaultEquipIcon(GameItemTag tag) {
        return switch (tag) {
            case GLOVES -> "🧤";
            case HELMET -> "⛑";
            case LEGS -> "👖";
            case ACCESSORY -> "💍";
            default -> "🛡";
        };
    }

    private String equipTypeLabel(GameItemTag tag) {
        return tag != null ? tag.getLabel() : "装备";
    }

    private String armorIdPrefix(GameItemTag tag) {
        return switch (tag) {
            case GLOVES -> "glo_";
            case HELMET -> "hel_";
            case LEGS -> "leg_";
            case ACCESSORY -> "acc_";
            default -> "arm_";
        };
    }

    private String generateUniqueArmorId(GameItemTag tag, String code) {
        if (code != null && !code.isBlank()) {
            String base = armorIdPrefix(tag) + code.trim().toLowerCase();
            if (armorService.getById(base) == null) {
                return base;
            }
        }
        String id;
        do {
            id = armorIdPrefix(tag) + WordUnit.randomKey(8, 3);
        } while (armorService.getById(id) != null);
        return id;
    }

    private AdminMaterialVo toMaterialVo(GameItem item) {
        AdminMaterialVo vo = new AdminMaterialVo();
        vo.setId(item.getId());
        vo.setCode(item.getCode());
        vo.setName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setMaxStack(item.getMaxStack());
        vo.setSort(item.getSort());
        vo.setEnabled(item.getEnabled());
        vo.setRemark(item.getRemark());
        return vo;
    }

    private AdminWeaponVo toWeaponVo(GameWeapon weapon, GameItem item) {
        AdminWeaponVo vo = new AdminWeaponVo();
        vo.setId(weapon.getId());
        vo.setItemId(item.getId());
        vo.setCode(item.getCode());
        vo.setName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setMaxStack(item.getMaxStack());
        vo.setSort(item.getSort());
        vo.setAttack(weapon.getAttack());
        vo.setBaseActionValue(weapon.getBaseActionValue());
        vo.setDamageRatio(weapon.getDamageRatio());
        vo.setConsumable(weapon.getConsumable() != null ? weapon.getConsumable() : 0);
        vo.setMaxUses(weapon.getMaxUses());
        vo.setEnabled(weapon.getEnabled());
        vo.setRemark(weapon.getRemark() != null ? weapon.getRemark() : item.getRemark());
        return vo;
    }

    private AdminArmorVo toArmorVo(GameArmor armor, GameItem item) {
        AdminArmorVo vo = new AdminArmorVo();
        vo.setId(armor.getId());
        vo.setItemId(item.getId());
        vo.setCode(item.getCode());
        vo.setName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setMaxStack(item.getMaxStack());
        vo.setSort(item.getSort());
        vo.setBonusHp(armor.getBonusHp());
        vo.setDefense(armor.getDefense());
        vo.setBonusAttack(armor.getBonusAttack());
        vo.setEnabled(armor.getEnabled());
        vo.setRemark(armor.getRemark() != null ? armor.getRemark() : item.getRemark());
        return vo;
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

    private String generateUniqueWeaponId() {
        String id;
        do {
            id = "wpn_" + WordUnit.randomKey(8, 3);
        } while (weaponService.getById(id) != null);
        return id;
    }

    private String generateUniqueItemId() {
        String id;
        do {
            id = "item_" + WordUnit.randomKey(8, 3);
        } while (itemService.getById(id) != null);
        return id;
    }
}
