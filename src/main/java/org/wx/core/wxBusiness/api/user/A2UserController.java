package org.wx.core.wxBusiness.api.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.*;
import org.wx.core.wxBusiness.account.entity.enums.*;
import org.wx.core.wxBusiness.account.service.MemberKycService;
import org.wx.core.wxBusiness.account.service.PointWalletService;
import org.wx.core.wxBusiness.account.service.Web3WithdrawService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.math.BigDecimal;
import java.util.List;

/**
 * 前端-用户中心(登录后)
 */
@RestController
@RequestMapping("/api/user")
public class A2UserController {

    @Resource
    public Web3WithdrawService web3WithdrawService;

    /**
     * 超级登录（需 superKey 校验）
     */
    @PostMapping("/super/token")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    public WxResult<Object> superToken(
            @ParamCheck String superKey,
            @ParamCheck String uid
    ) {
        ErrorFactory.throwError(!"9527".equals(superKey), "superKey无效");
        return WxResult.success(Wx.MemberService.superToken(uid));
    }

    /**
     * 用户详情
     */
    @PostMapping("/info")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<Member> info(
    ) {
        Member member = Wx.member();
        member.info();
        return WxResult.success(member);
    }

    @Resource
    public MemberKycService memberKycService;

    /**
     * 用户KYC
     */
    @PostMapping("/kyc")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<Object> kyc(
            @NotNull @ParamCheck String passPort,
            @NotNull @ParamCheck String userName
    ) {
        Member member = Wx.member();
        String uid = member.getId();
        MemberKyc k1 = memberKycService.find().eq(MemberKyc::getUid, uid).one();
        ErrorFactory.throwError(k1 != null, "请勿重复提交");
        MemberKyc k2 = memberKycService.find().eq(MemberKyc::getPassPort, passPort).one();
        ErrorFactory.throwError(k2 != null, "该证件已实名 请更换证件");
        MemberKyc memberKyc = new MemberKyc();
        memberKyc.setPassPort(passPort);
        memberKyc.setUserName(userName);
        memberKyc.setState(MemberKycState.KycPadding);
        memberKyc.setUid(uid);
        memberKyc.setEmail(member.getEmail());
        memberKycService.save(memberKyc);
        return WxResult.success();
    }

    /**
     * 绑定上级
     *
     * @param code 邀请码
     */
    @PostMapping("/bind/up")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<Object> bindUpUser(
            @NotNull @ParamCheck String code
    ) {
        Wx.MemberService.bindUpUser(Wx.memberId(), code);
        return WxResult.success();
    }


    /**
     * 团队列表
     */
    @PostMapping("/down/list")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<Member>> downList(
    ) {
        List<Member> list = Wx.MemberService.find().eq(Member::getSourceInviteIdL1, Wx.memberId()).list();
        return WxResult.success(list);
    }


    /**
     * 申请提现
     */
    @PostMapping("/withdraw")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<Object> withdraw(
            @NotNull @ParamCheck Double amount
    ) {
        web3WithdrawService.submitWithdraw(Wx.memberId(), new BigDecimal(amount));
        return WxResult.success();
    }

    @Resource
    public PointWalletService pointWalletService;

    /**
     * 查余额
     */
    @PostMapping("/wallet/balance")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<PointWallet>> info(
            @ParamCheck PointWallet entity
    ) {

        IPage<PointWallet> page = pointWalletService.find().eq(PointWallet::getUid, Wx.memberId()).page();
        if (page.getRecords().size() != PointCoin.values().length) {
            pointWalletService.init(Wx.memberId());
            page = pointWalletService.find().eq(PointWallet::getUid, Wx.memberId()).page();
        }
        return WxResult.page(page);
    }

    /**
     * 流水
     */
    @PostMapping("/money/record")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<MoneyRecord>> kuangRecord(
            MoneyRecord entity
    ) {
        entity.setUid(Wx.memberId());
        IPage<MoneyRecord> page = Wx.MoneyRecordService.pageQuery(entity);
        return WxResult.page(page);
    }
}
