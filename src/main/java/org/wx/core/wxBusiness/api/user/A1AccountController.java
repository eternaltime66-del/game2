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

    /**
     * 邮箱注册
     */
    @PostMapping("/email/register")
    @WxRequestLog()
    public WxResult<String> emailRegister(
            @ParamCheck(msg = "邮箱") String email,
            @ParamCheck(msg = "验证码") String emsCode,
            @ParamCheck(msg = "密码") String psd,
            @ParamCheck(msg = "确认密码") String psdAgain
    ) {
        String token = Wx.MemberService.signUpEmailAccountForPsd(email, emsCode, psd, psdAgain);
        return WxResult.token(token);
    }

    /**
     * 邮箱密码登录
     */
    @PostMapping("/email/login")
    @WxRequestLog()
    public WxResult<String> emailLogin(
            @ParamCheck(msg = "邮箱") String email,
            @ParamCheck(msg = "密码") String password
    ) {
        String token = Wx.MemberService.signInEmailAccountForPsd(email, password);
        return WxResult.token(token);
    }

    /**
     * 邮箱验证码登录
     */
    @PostMapping("/email/login/code")
    @WxRequestLog()
    public WxResult<String> emailLoginByCode(
            @ParamCheck(msg = "邮箱") String email,
            @ParamCheck(msg = "验证码") String emsCode
    ) {
        String token = Wx.MemberService.signInEmailAccountForeMms(email, emsCode);
        return WxResult.token(token);
    }

    /**
     * 忘记密码
     */
    @PostMapping("/email/forget")
    @WxRequestLog()
    public WxResult<String> emailForget(
            @ParamCheck(msg = "邮箱") String email,
            @ParamCheck(msg = "验证码") String emsCode,
            @ParamCheck(msg = "密码") String psd,
            @ParamCheck(msg = "确认密码") String psdAgain
    ) {
        Wx.MemberService.forgetEmailAccountForEms(email, emsCode, psd, psdAgain);
        return WxResult.success();
    }
}
