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
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.api.vo.CommonIdVo;
import org.wx.core.wxBusiness.game.entity.GameItemTrigger;
import org.wx.core.wxBusiness.game.entity.TriggerOptionVo;
import org.wx.core.wxBusiness.game.service.GameItemTriggerAdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-物品扳机
 */
@RestController
@RequestMapping("/back/game/trigger")
public class B13GameItemTriggerController {

    @Resource
    private GameItemTriggerAdminService triggerAdminService;

    @PostMapping("/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameItemTrigger>> list(@RequestBody GameItemTrigger entity) {
        entity.clearEmptyString();
        IPage<GameItemTrigger> page = triggerAdminService.list(entity);
        return WxResult.page(page);
    }

    @PostMapping("/by-item")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameItemTrigger>> listByItem(@ParamCheck(msg = "物品ID") String itemId) {
        return WxResult.success(triggerAdminService.listByItemId(itemId));
    }

    @PostMapping("/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<GameItemTrigger> save(@RequestBody GameItemTrigger entity) {
        return WxResult.success(triggerAdminService.save(entity));
    }

    @PostMapping("/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> remove(@RequestBody CommonIdVo vo) {
        triggerAdminService.remove(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/phase/options")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> phaseOptions() {
        return WxResult.success(triggerAdminService.listPhaseOptions());
    }
}
