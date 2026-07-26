package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.Web3Withdraw;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
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
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<Web3Withdraw>> defList(
            @RequestBody Web3Withdraw entity
    ){
        return WxResult.page(web3WithdrawService.pageQuery(entity));
    }

    /**
     * 成功
     */
    @PostMapping("/success")
//    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<Web3Withdraw>> success(
            @RequestBody Web3Withdraw entity
    ){

        web3WithdrawService.success(entity.getId().toString(), entity.getHash());
        return WxResult.success();
    }

    /**
     * 失败
     */
    @PostMapping("/fail")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<Web3Withdraw>> fail(
            @RequestBody Web3Withdraw entity
    ){
        web3WithdrawService.fail(entity.getId().toString(),entity.getHash());
        return WxResult.success();
    }


    /**
     * 总AI资产
     *
     */
    @PostMapping("/total/waiting")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<Object> totalAi(
    ) {
        QueryWrapper<Web3Withdraw> wrapper = new QueryWrapper<>();
        wrapper.eq("state","待审核");
        String balance = web3WithdrawService.sumQuery(wrapper, "amount","amount");
        return WxResult.success(balance);
    }


}
