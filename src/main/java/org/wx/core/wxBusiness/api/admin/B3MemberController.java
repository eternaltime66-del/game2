package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.MemberKyc;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.account.service.MemberKycService;
import org.wx.core.wxBusiness.account.service.MemberService;
import org.wx.core.wxBusiness.account.service.Web3WithdrawService;
import org.wx.core.wxBusiness.api.vo.CommonIdVo;
import org.wx.core.wxBusiness.api.vo.MemberAdminGrantVo;
import org.wx.core.wxBusiness.api.vo.MemberPersonSkillOptionVo;
import org.wx.core.wxBusiness.game.service.GameTriggerV2AdminService;
import org.wx.core.wxBusiness.game.service.GameUserAdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;
import org.wx.core.wxBase.factory.ErrorFactory;

import java.util.List;

/**
 * 后台-普通用户
 */
@RestController
@RequestMapping("/back/member")
public class B3MemberController {

    @Resource
    public MemberService memberService;

    @Resource
    public MemberKycService memberKycService;
    @Resource
    public Web3WithdrawService web3WithdrawService;
    @Resource
    private GameUserAdminService gameUserAdminService;
    @Resource
    private GameTriggerV2AdminService gameTriggerV2AdminService;

    /**
     * 用户列表
     */
    @PostMapping("/list")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<Member>> defList(
            @RequestBody Member entity
    ) {
        return WxResult.page(memberService.pageQuery(entity));
    }


    /**
     * 整条线设置 TO_ADDRESS
     */
    @PostMapping("/set/to/address")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<Member>> setToAddress(
            @RequestBody Member entity
    ) {
        LambdaUpdateWrapper<Member> wrapper = new LambdaUpdateWrapper<>();
        wrapper.like(Member::getSourceInviteIds,entity.getSourceInviteIdL1());
        wrapper.set(Member::getToAddress,entity.getToAddress());
        Wx.MemberService.update(wrapper);
        return WxResult.success();
    }



    /**
     * 用户KYC列表
     */
    @PostMapping("/kyc/list")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<MemberKyc>> kycList(
            @RequestBody MemberKyc entity
    ) {
        return WxResult.page(memberKycService.pageQuery(entity));
    }

    /**
     * 用户KYC 审核
     */
    @PostMapping("/kyc/action")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<MemberKyc>> kycAction(
            @RequestBody MemberKyc entity
    ) {
        memberKycService.updateById(entity);
        return WxResult.success();
    }

    /**
     * 一键登录用户（代登录）
     */
    @PostMapping("/login/as")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<String> loginAs(
            @RequestBody Member entity
    ) {
        return WxResult.token(Wx.MemberService.superToken(entity.getId()));
    }

    /**
     * 清空用户游戏数据（仓库、背包、装备、角色、物品/资金流水、战斗缓存等）
     */
    @PostMapping("/reset/game")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<?> resetGameData(@RequestBody CommonIdVo vo) {
        gameUserAdminService.resetGameData(vo.stringId());
        return WxResult.success();
    }

    /**
     * 后台赠送物品到用户仓库
     */
    @PostMapping("/grant/item")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<?> grantItem(@RequestBody MemberAdminGrantVo vo) {
        ErrorFactory.notNull(vo.getId(), "用户ID不能为空");
        ErrorFactory.notNull(vo.getItemId(), "请选择物品");
        int quantity = vo.getQuantity() != null ? vo.getQuantity() : 0;
        gameUserAdminService.grantItem(vo.getId(), vo.getItemId(), quantity);
        return WxResult.success();
    }

    /**
     * 可赠送的人物主动技能列表
     */
    @PostMapping("/grant/person-skill/options")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<MemberPersonSkillOptionVo>> personSkillGrantOptions() {
        return WxResult.success(gameTriggerV2AdminService.listPersonActiveGrantOptions());
    }

    /**
     * 后台赠送人物主动技能到用户仓库（技能物品，可装备到技能槽）
     */
    @PostMapping("/grant/person-skill")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<?> grantPersonSkill(@RequestBody MemberAdminGrantVo vo) {
        ErrorFactory.notNull(vo.getId(), "用户ID不能为空");
        ErrorFactory.notNull(vo.getFinishedSkillId(), "请选择人物技能");
        int quantity = vo.getQuantity() != null ? vo.getQuantity() : 1;
        gameUserAdminService.grantPersonSkill(vo.getId(), vo.getFinishedSkillId(), quantity);
        return WxResult.success();
    }

}
