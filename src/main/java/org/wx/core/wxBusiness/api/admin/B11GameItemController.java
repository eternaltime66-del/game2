package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.api.vo.CommonIdVo;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.service.GameInventoryService;
import org.wx.core.wxBusiness.game.service.GameItemCategoryAdminService;
import org.wx.core.wxBusiness.game.service.GameItemService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;
import java.util.stream.Collectors;

/** 后台-物品管理 */
@RestController
@RequestMapping("/back/game/item")
public class B11GameItemController {

    @Resource
    private GameItemService itemService;

    @Resource
    private GameItemCategoryAdminService categoryAdminService;

    @Resource
    private GameInventoryService inventoryService;

    /** 全部物品（扳机槽等下拉用） */
    @PostMapping("/all/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameItem>> allList() {
        return WxResult.success(categoryAdminService.listAllItemsForPicker());
    }

    @PostMapping("/material/list")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminMaterialVo>> materialList(@RequestBody GameItem entity) {
        return WxResult.page(categoryAdminService.listMaterials(entity));
    }

    @PostMapping("/material/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminMaterialVo> materialSave(@RequestBody AdminMaterialVo vo) {
        return WxResult.success(categoryAdminService.saveMaterial(vo));
    }

    @PostMapping("/material/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> materialRemove(@RequestBody CommonIdVo vo) {
        categoryAdminService.removeMaterial(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/weapon/list")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminWeaponVo>> weaponList(@RequestBody GameWeapon entity) {
        return WxResult.page(categoryAdminService.listWeapons(entity));
    }

    @PostMapping("/weapon/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminWeaponVo> weaponSave(@RequestBody AdminWeaponVo vo) {
        return WxResult.success(categoryAdminService.saveWeapon(vo));
    }

    @PostMapping("/weapon/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> weaponRemove(@RequestBody CommonIdVo vo) {
        categoryAdminService.removeWeapon(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/armor/list")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminArmorVo>> armorList(@RequestBody GameArmor entity) {
        return WxResult.page(categoryAdminService.listArmors(entity));
    }

    @PostMapping("/armor/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminArmorVo> armorSave(@RequestBody AdminArmorVo vo) {
        return WxResult.success(categoryAdminService.saveArmor(vo));
    }

    @PostMapping("/armor/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> armorRemove(@RequestBody CommonIdVo vo) {
        categoryAdminService.removeArmor(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/gloves/list")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminArmorVo>> glovesList(@RequestBody GameArmor entity) {
        return WxResult.page(categoryAdminService.listGloves(entity));
    }

    @PostMapping("/gloves/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminArmorVo> glovesSave(@RequestBody AdminArmorVo vo) {
        return WxResult.success(categoryAdminService.saveGloves(vo));
    }

    @PostMapping("/gloves/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> glovesRemove(@RequestBody CommonIdVo vo) {
        categoryAdminService.removeArmor(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/helmet/list")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminArmorVo>> helmetList(@RequestBody GameArmor entity) {
        return WxResult.page(categoryAdminService.listHelmets(entity));
    }

    @PostMapping("/helmet/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminArmorVo> helmetSave(@RequestBody AdminArmorVo vo) {
        return WxResult.success(categoryAdminService.saveHelmet(vo));
    }

    @PostMapping("/helmet/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> helmetRemove(@RequestBody CommonIdVo vo) {
        categoryAdminService.removeArmor(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/legs/list")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminArmorVo>> legsList(@RequestBody GameArmor entity) {
        return WxResult.page(categoryAdminService.listLegs(entity));
    }

    @PostMapping("/legs/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminArmorVo> legsSave(@RequestBody AdminArmorVo vo) {
        return WxResult.success(categoryAdminService.saveLegs(vo));
    }

    @PostMapping("/legs/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> legsRemove(@RequestBody CommonIdVo vo) {
        categoryAdminService.removeArmor(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/accessory/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminArmorVo>> accessoryList(@RequestBody GameArmor entity) {
        return WxResult.page(categoryAdminService.listAccessories(entity));
    }

    @PostMapping("/accessory/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminArmorVo> accessorySave(@RequestBody AdminArmorVo vo) {
        return WxResult.success(categoryAdminService.saveAccessory(vo));
    }

    @PostMapping("/accessory/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> accessoryRemove(@RequestBody CommonIdVo vo) {
        categoryAdminService.removeArmor(vo.stringId());
        return WxResult.success();
    }

    @Deprecated
    @PostMapping("/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameItem>> list(@RequestBody GameItem entity) {
        entity.clearEmptyString();
        IPage<GameItem> page = itemService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> save(@RequestBody GameItem entity) {
        entity.clearEmptyString();
        ErrorFactory.notNull(entity.getCode(), "编码不能为空");
        ErrorFactory.notNull(entity.getName(), "名称不能为空");
        if (entity.getMaxStack() == null) {
            entity.setMaxStack(99);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getItemTags() == null || entity.getItemTags().isBlank()) {
            entity.setItemTags(GameItemTag.MATERIAL.name());
        }
        Integer oldMaxStack = null;
        if (entity.getId() == null || entity.getId().isBlank()) {
            itemService.save(entity);
        } else {
            GameItem exists = itemService.getById(entity.getId());
            if (exists != null) {
                oldMaxStack = exists.getMaxStack();
            }
            itemService.updateById(entity);
        }
        if (oldMaxStack != null && entity.getMaxStack() != null && !oldMaxStack.equals(entity.getMaxStack())) {
            inventoryService.restackItemInAllWarehouses(entity.getId());
        }
        return WxResult.success();
    }

    @PostMapping("/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> remove(@RequestBody CommonIdVo vo) {
        itemService.removeById(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/tag/options")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<ItemTagOptionVo>> tagOptions() {
        List<ItemTagOptionVo> options = GameItemTag.allSorted().stream()
                .map(tag -> {
                    ItemTagOptionVo vo = new ItemTagOptionVo();
                    vo.setCode(tag.name());
                    vo.setLabel(tag.getLabel());
                    vo.setSort(tag.getSort());
                    return vo;
                })
                .collect(Collectors.toList());
        return WxResult.success(options);
    }
}
