package org.wx.core.wxBusiness.api.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.game.entity.GameItemLog;
import org.wx.core.wxBusiness.game.entity.WarehouseVo;
import org.wx.core.wxBusiness.game.service.GameInventoryService;
import org.wx.core.wxBusiness.game.service.GameItemLogService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 前端-仓库与物品
 */
@RestController
@RequestMapping("/api/game/warehouse")
public class A4GameWarehouseController {

    @Resource
    private GameInventoryService inventoryService;
    @Resource
    private GameItemLogService itemLogService;

    /**
     * 仓库详情（含全部格子）
     */
    @PostMapping("/info")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<WarehouseVo> info() {
        return WxResult.success(inventoryService.getWarehouseDetail(Wx.memberId()));
    }

    /**
     * 丢弃选中格子物品
     */
    @PostMapping("/discard")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<?> discard(
            @ParamCheck(msg = "格子序号") String slotNos
    ) {
        List<Integer> slots = parseSlotNos(slotNos);
        inventoryService.discardSlots(Wx.memberId(), slots);
        return WxResult.success();
    }

    /**
     * 物品变动日志
     */
    @PostMapping("/log/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<GameItemLog>> logList() {
        IPage<GameItemLog> page = itemLogService.find()
                .eq(GameItemLog::getUid, Wx.memberId())
                .orderByDesc(GameItemLog::getCreateTime)
                .page();
        return WxResult.page(page);
    }

    private List<Integer> parseSlotNos(String slotNos) {
        return Arrays.stream(slotNos.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }
}
