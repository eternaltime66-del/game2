package org.wx.core.wxBusiness.api.admin;

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
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.service.GamePassiveSkillAdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

@RestController
@RequestMapping("/back/game/passive-skill")
public class B17GamePassiveSkillController {

    @Resource
    private GamePassiveSkillAdminService passiveSkillAdminService;

    @PostMapping("/meta")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<PassiveSkillMetaVo> meta() {
        return WxResult.success(passiveSkillAdminService.meta());
    }

    @PostMapping("/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminPassiveSkillVo>> list(@RequestBody GamePassiveSkill query) {
        return WxResult.page(passiveSkillAdminService.listPassiveSkills(query));
    }

    @PostMapping("/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminPassiveSkillVo> save(@RequestBody AdminPassiveSkillVo vo) {
        return WxResult.success(passiveSkillAdminService.savePassiveSkill(vo));
    }

    @PostMapping("/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> remove(@RequestBody CommonIdVo vo) {
        passiveSkillAdminService.removePassiveSkill(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/badge/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminSkillBadgeVo>> badgeList(@RequestBody GameItem query) {
        return WxResult.page(passiveSkillAdminService.listSkillBadges(query));
    }

    @PostMapping("/badge/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminSkillBadgeVo> badgeSave(@RequestBody AdminSkillBadgeVo vo) {
        return WxResult.success(passiveSkillAdminService.saveSkillBadge(vo));
    }

    @PostMapping("/badge/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> badgeRemove(@RequestBody CommonIdVo vo) {
        passiveSkillAdminService.removeSkillBadge(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/item-passive/by-item")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminItemPassiveVo>> itemPassiveByItem(@ParamCheck(msg = "物品ID") String itemId) {
        return WxResult.success(passiveSkillAdminService.listItemPassivesByItem(itemId));
    }

    @PostMapping("/item-passive/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminItemPassiveVo> itemPassiveSave(@RequestBody AdminItemPassiveVo vo) {
        return WxResult.success(passiveSkillAdminService.saveItemPassive(vo));
    }

    @PostMapping("/item-passive/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> itemPassiveRemove(@RequestBody CommonIdVo vo) {
        passiveSkillAdminService.removeItemPassive(vo.stringId());
        return WxResult.success();
    }
}
