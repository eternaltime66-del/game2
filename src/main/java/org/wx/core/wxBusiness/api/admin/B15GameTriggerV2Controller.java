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
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.service.GameTriggerV2AdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/** 后台-扳机 v2（成品技能 / 完整技能组 / 扳机槽） */
@RestController
@RequestMapping("/back/game/trigger-v2")
public class B15GameTriggerV2Controller {

    @Resource
    private GameTriggerV2AdminService adminService;

    @PostMapping("/finished-skill/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameFinishedSkill>> finishedSkillList(@RequestBody GameFinishedSkill entity) {
        entity.clearEmptyString();
        IPage<GameFinishedSkill> page = adminService.listFinishedSkills(entity);
        return WxResult.page(page);
    }

    @PostMapping("/finished-skill/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminFinishedSkillVo> finishedSkillDetail(@ParamCheck(msg = "技能ID") String id) {
        return WxResult.success(adminService.getFinishedSkillDetail(id));
    }

    @PostMapping("/finished-skill/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminFinishedSkillVo> finishedSkillSave(@RequestBody AdminFinishedSkillVo vo) {
        return WxResult.success(adminService.saveFinishedSkill(vo));
    }

    @PostMapping("/finished-skill/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> finishedSkillRemove(@RequestBody CommonIdVo vo) {
        adminService.removeFinishedSkill(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/complete-skill/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminCompleteSkillVo>> completeSkillList(@RequestBody GameCompleteSkill entity) {
        entity.clearEmptyString();
        IPage<AdminCompleteSkillVo> page = adminService.listCompleteSkills(entity);
        return WxResult.page(page);
    }

    @PostMapping("/complete-skill/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminCompleteSkillVo> completeSkillSave(@RequestBody AdminCompleteSkillVo vo) {
        return WxResult.success(adminService.saveCompleteSkill(vo));
    }

    @PostMapping("/complete-skill/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> completeSkillRemove(@RequestBody CommonIdVo vo) {
        adminService.removeCompleteSkill(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/trigger-slot/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminTriggerSlotVo>> triggerSlotList(@RequestBody GameTriggerSlot entity) {
        entity.clearEmptyString();
        IPage<AdminTriggerSlotVo> page = adminService.listTriggerSlots(entity);
        return WxResult.page(page);
    }

    @PostMapping("/trigger-slot/by-item")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminTriggerSlotVo>> triggerSlotByItem(@ParamCheck(msg = "物品ID") String itemId) {
        return WxResult.success(adminService.listTriggerSlotsByItem(itemId));
    }

    @PostMapping("/trigger-slot/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminTriggerSlotVo> triggerSlotSave(@RequestBody AdminTriggerSlotVo vo) {
        return WxResult.success(adminService.saveTriggerSlot(vo));
    }

    @PostMapping("/trigger-slot/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> triggerSlotRemove(@RequestBody CommonIdVo vo) {
        adminService.removeTriggerSlot(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/meta/trigger-types")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> triggerTypes() {
        return WxResult.success(adminService.listTriggerSlotTypeOptions());
    }

    @PostMapping("/meta/target-types")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> targetTypes() {
        return WxResult.success(adminService.listTargetTypeOptions());
    }

    @PostMapping("/meta/effect-kinds")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> effectKinds() {
        return WxResult.success(adminService.listEffectKindOptions());
    }

    @PostMapping("/meta/stat-refs")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> statRefs() {
        return WxResult.success(adminService.listStatRefOptions());
    }

    @PostMapping("/meta/outcome-types")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> outcomeTypes() {
        return WxResult.success(adminService.listOutcomeTypeOptions());
    }

    @PostMapping("/meta/bind-types")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> bindTypes() {
        return WxResult.success(adminService.listBindTypeOptions());
    }

    @PostMapping("/meta/finished-skill-options")
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameFinishedSkill>> finishedSkillOptions() {
        return WxResult.success(adminService.listFinishedSkillOptions());
    }
}
