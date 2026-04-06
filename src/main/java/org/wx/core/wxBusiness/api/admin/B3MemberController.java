package org.wx.core.wxBusiness.api.admin;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.MemberKyc;
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
    public WxResult<List<Member>> defList(
            @RequestBody Member entity
    ) {
        return WxResult.page(memberService.pageQuery(entity));
    }




    /**
     * 用户KYC列表
     */
    @PostMapping("/kyc/list")
    @WxRequestLog()
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
    public WxResult<List<MemberKyc>> kycAction(
            @RequestBody MemberKyc entity
    ) {
        memberKycService.updateById(entity);
        return WxResult.success();
    }

}
