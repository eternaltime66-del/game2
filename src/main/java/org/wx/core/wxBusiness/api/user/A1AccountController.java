package org.wx.core.wxBusiness.api.user;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.service.MemberService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

/**
 * 前端-用户账户注册&登录
 */
@RestController
@RequestMapping("/api/account")
public class A1AccountController {

    /**
     * 获取授权码
     *
     * @param address 用户地址
     * @return
     */
    @PostMapping("/logon/getCode")
    @WxRequestLog()
    public WxResult<String> logonRobotGetCode(
            @ParamCheck String address
    ) {
        return WxResult.success(Wx.MemberService.getAuthCode(address));
    }

    /**
     * 注册&登录
     *
     * @param address
     * @param signature
     * @param code
     * @return
     */
    @PostMapping("/logon/getAccount")
    @WxRequestLog()
    public WxResult<String> logonRobotBySign(
            @ParamCheck String address,
            @ParamCheck String signature,
            @ParamCheck String code
    ) {
        return WxResult.success(Wx.MemberService.getAccount(address, signature, code));
    }




}
