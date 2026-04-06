package org.wx.core.wxBusiness.api.admin;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.Web3Withdraw;
import org.wx.core.wxBusiness.account.service.Web3WithdrawService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-提现
 */
@RestController
@RequestMapping("/back/withdraw")
public class B2WithdrawController {

    @Resource
    public Web3WithdrawService web3WithdrawService;

    /**
     * 列表
     */
    @PostMapping("/list")
    @WxRequestLog()
    public WxResult<List<Web3Withdraw>> defList(
            @RequestBody Web3Withdraw entity
    ){
        return WxResult.page(web3WithdrawService.pageQuery(entity));
    }

    /**
     * 成功
     */
    @PostMapping("/success")
    @WxRequestLog()
    public WxResult<List<Web3Withdraw>> success(
            @RequestBody Web3Withdraw entity
    ){
        web3WithdrawService.success(entity.getId().toString(),entity.getHash());
        return WxResult.success();
    }

    /**
     * 失败
     */
    @PostMapping("/fail")
    @WxRequestLog()
    public WxResult<List<Web3Withdraw>> fail(
            @RequestBody Web3Withdraw entity
    ){
        web3WithdrawService.fail(entity.getId().toString(),entity.getHash());
        return WxResult.success();
    }

}
