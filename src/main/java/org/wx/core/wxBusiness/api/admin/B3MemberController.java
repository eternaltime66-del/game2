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
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

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
     * 整条线设置 锁定登录
     */
    @PostMapping("/set/lock")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<Member>> setLock(
            @RequestBody Member entity
    ) {
        LambdaUpdateWrapper<Member> wrapper = new LambdaUpdateWrapper<>();
        wrapper.like(Member::getSourceInviteIds,entity.getSourceInviteIdL1());
        wrapper.set(Member::getLock,entity.getLock());
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

}
