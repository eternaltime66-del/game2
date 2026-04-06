package org.wx.core.wxBusiness.api.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Member;

import java.util.List;

@RestController
public class TestController {

    /**
     * 正常接口
     */
    @WxRequestLog(module = "测试接口1")
    @GetMapping("/test/ok")
    public WxResult<String> ok() {
        return WxResult.success("ok");
    }

    /**
     * 模拟 token 无效
     */
    @WxRequestLog(module = "测试接口2")
    @GetMapping("/test/token")
    public WxResult<Object> tokenError() {
        ErrorFactory.throwError("123");
        return null;
    }

    /**
     * 模拟运行时异常
     */
    @WxRequestLog(module = "测试接口3")
    @GetMapping("/test/runtime")
    public WxResult<Object> runtimeError() {
        int i = 1 / 0;
        return WxResult.success(i);
    }

    /**
     * 用户列表
     */
    @PostMapping("/user/list")
    @WxRequestLog(module = "用户列表")
    public WxResult<List<Member>> userList(
           @RequestBody Member dto
    ) {
        return WxResult.page(Wx.MemberService.pageQuery(dto));
    }

    /**
     * 修改 用户
     */
    @PostMapping("/user/update")
    @WxRequestLog(module = "用户列表",recordDataChange=true)
    public WxResult<Object> userUpdate(
            @RequestBody Member entity
    ) {
        Wx.MemberService.updateById(entity);
        return WxResult.success();
    }
}
