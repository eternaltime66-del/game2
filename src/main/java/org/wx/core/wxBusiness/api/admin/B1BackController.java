package org.wx.core.wxBusiness.api.admin;


import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台接口
 */
@RestController
@RequestMapping("/back")
public class B1BackController {

    /**
     * 后台用户登录
     *
     * @param account  账户
     * @param password 密码
     * @return Token
     */
    @PostMapping("/login")
    @WxRequestLog()
    public WxResult<Object> login(
            @NotNull @ParamCheck String account,
            @NotNull @ParamCheck String password
    ){
        String token = Wx.MemberService.signInAdminForPsd(account, password);
        return WxResult.token(token);
    }

    /**
     * 模拟登录info
     */
    @PostMapping("/text/login/info")
    @WxRequestLog
    @NeedHeader(roles ={MemberRole.ADMIN})
    public WxResult<Object> testLoginInfo(
    ) {
        Member member = Wx.member();
        List<String> roles = new ArrayList<>();
        roles.add("admin");

        ElementLoginUser user = new ElementLoginUser(
                roles,
                "I am a super administrator",
                "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif",
                member.getName()
        );

        return WxResult.success(user);
    }

    @Data
    public static class ElementLoginUser {
        private List<String> roles;
        private String introduction;
        private String avatar;
        private String name;

        public ElementLoginUser(List<String> roles, String introduction, String avatar, String name) {
            this.roles = roles;
            this.introduction = introduction;
            this.avatar = avatar;
            this.name = name;
        }
    }

}

