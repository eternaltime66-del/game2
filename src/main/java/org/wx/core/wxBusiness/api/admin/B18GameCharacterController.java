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
import org.wx.core.wxBusiness.game.entity.GameCharacterTemplate;
import org.wx.core.wxBusiness.game.entity.GameProfession;
import org.wx.core.wxBusiness.game.entity.GameProfessionSkill;
import org.wx.core.wxBusiness.game.service.GameCharacterAdminService;
import org.wx.core.wxBusiness.game.service.GameProfessionAdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

@RestController
@RequestMapping("/back/game/character")
public class B18GameCharacterController {

    @Resource
    private GameCharacterAdminService characterAdminService;
    @Resource
    private GameProfessionAdminService professionAdminService;

    @PostMapping("/template/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameCharacterTemplate>> templateList() {
        return WxResult.success(characterAdminService.listTemplates());
    }

    @PostMapping("/template/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<GameCharacterTemplate> templateSave(@RequestBody GameCharacterTemplate entity) {
        return WxResult.success(characterAdminService.saveTemplate(entity));
    }

    @PostMapping("/profession/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameProfession>> professionList() {
        return WxResult.success(professionAdminService.listProfessions());
    }

    @PostMapping("/profession/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> professionSave(@RequestBody GameProfession entity) {
        professionAdminService.saveProfession(entity);
        return WxResult.success();
    }

    @PostMapping("/profession/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> professionRemove(@RequestBody CommonIdVo vo) {
        professionAdminService.removeProfession(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/profession/skill/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameProfessionSkill>> professionSkillList(
            @ParamCheck(msg = "职业ID") String professionId
    ) {
        return WxResult.success(professionAdminService.listProfessionSkills(professionId));
    }

    @PostMapping("/profession/skill/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> professionSkillSave(@RequestBody GameProfessionSkill entity) {
        professionAdminService.saveProfessionSkill(entity);
        return WxResult.success();
    }

    @PostMapping("/profession/skill/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> professionSkillRemove(@RequestBody CommonIdVo vo) {
        professionAdminService.removeProfessionSkill(vo.stringId());
        return WxResult.success();
    }
}
