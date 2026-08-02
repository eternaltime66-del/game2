package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.api.vo.CommonIdVo;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameMonsterDrop;
import org.wx.core.wxBusiness.game.entity.ItemTagOptionVo;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.service.GameItemService;
import org.wx.core.wxBusiness.game.service.GameMonsterDropService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台-物品与掉落配置
 */
@RestController
@RequestMapping("/back/game/item")
public class B11GameItemController {

    @Resource
    private GameItemService itemService;
    @Resource
    private GameMonsterDropService gameMonsterDropService;

    @PostMapping("/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameItem>> itemList(@RequestBody GameItem entity) {
        entity.clearEmptyString();
        IPage<GameItem> page = itemService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> itemSave(@RequestBody GameItem entity) {
        entity.clearEmptyString();
        ErrorFactory.notNull(entity.getCode(), "编码不能为空");
        ErrorFactory.notNull(entity.getName(), "名称不能为空");
        if (entity.getMaxStack() == null) {
            entity.setMaxStack(99);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getId() == null || entity.getId().isBlank()) {
            itemService.save(entity);
        } else {
            itemService.updateById(entity);
        }
        return WxResult.success();
    }

    @PostMapping("/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> itemRemove(@RequestBody CommonIdVo vo) {
        itemService.removeById(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/drop/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameMonsterDrop>> dropList(@RequestBody GameMonsterDrop entity) {
        entity.clearEmptyString();
        IPage<GameMonsterDrop> page = gameMonsterDropService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/drop/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> dropSave(@RequestBody GameMonsterDrop entity) {
        entity.clearEmptyString();
        ErrorFactory.notNull(entity.getMonsterId(), "怪物ID不能为空");
        ErrorFactory.notNull(entity.getItemId(), "物品ID不能为空");
        if (entity.getDropRate() == null) {
            entity.setDropRate(100);
        }
        if (entity.getMinQty() == null) {
            entity.setMinQty(1);
        }
        if (entity.getMaxQty() == null) {
            entity.setMaxQty(1);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getId() == null || entity.getId().isBlank()) {
            gameMonsterDropService.save(entity);
        } else {
            gameMonsterDropService.updateById(entity);
        }
        return WxResult.success();
    }

    @PostMapping("/drop/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> dropRemove(@RequestBody CommonIdVo vo) {
        gameMonsterDropService.removeById(vo.stringId());
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
