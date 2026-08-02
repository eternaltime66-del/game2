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
import org.wx.core.wxBusiness.game.entity.AdminGameSkillVo;
import org.wx.core.wxBusiness.game.entity.GameSkill;
import org.wx.core.wxBusiness.game.entity.TriggerOptionVo;
import org.wx.core.wxBusiness.game.service.GameSkillAdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-完整技能
 */
@RestController
@RequestMapping("/back/game/skill")
public class B14GameSkillController {

    @Resource
    private GameSkillAdminService skillAdminService;

    @PostMapping("/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameSkill>> list(@RequestBody GameSkill entity) {
        entity.clearEmptyString();
        IPage<GameSkill> page = skillAdminService.list(entity);
        return WxResult.page(page);
    }

    @PostMapping("/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminGameSkillVo> detail(@ParamCheck(msg = "技能ID") String skillId) {
        return WxResult.success(skillAdminService.getDetail(skillId));
    }

    @PostMapping("/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminGameSkillVo> save(@RequestBody AdminGameSkillVo entity) {
        return WxResult.success(skillAdminService.save(entity));
    }

    @PostMapping("/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> remove(@RequestBody CommonIdVo vo) {
        skillAdminService.remove(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/effect/options")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> effectOptions() {
        return WxResult.success(skillAdminService.listEffectOptions());
    }

    @PostMapping("/target/options")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<TriggerOptionVo>> targetOptions() {
        return WxResult.success(skillAdminService.listTargetOptions());
    }
}
