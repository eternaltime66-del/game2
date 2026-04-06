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
import org.wx.core.wxBase.factory.PageFactory;
import org.wx.core.wxBusiness.account.entity.*;
import org.wx.core.wxBusiness.account.entity.enums.MemberKycState;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.account.service.MemberKycService;
import org.wx.core.wxBusiness.account.service.PointWalletService;
import org.wx.core.wxBusiness.account.service.Web3WithdrawService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.math.BigDecimal;
import java.util.HashMap;
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
     * 用户详情
     *
     * @return
     */
    @PostMapping("/info")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    public WxResult<Member> info(
    ) {
        Member member = Wx.member();
        member.info();
        MemberKyc kyc = memberKycService.find().eq(MemberKyc::getUid, member.getId()).one();
        member.getMore().setKycState(MemberKycState.NotKyc);
        if (kyc != null) {
            member.getMore().setKycState(kyc.getState());
        }

        member.getMore().setTdNum(Wx.MemberService.find().like(Member::getSourceInviteIds, member.getId()).count());
        member.getMore().setZtNum(Wx.MemberService.find().eq(Member::getSourceInviteIdL1, member.getId()).count());

        return WxResult.success(member);
    }

    @Resource
    public MemberKycService memberKycService;

    /**
     * 用户KYC
     */
    @PostMapping("/kyc")
    @WxRequestLog(recordRequest = false, recordResponse = false)
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
    public WxResult<List<Member>> downList(
    ) {
        List<Member> list = Wx.MemberService.find().eq(Member::getSourceInviteIdL1, Wx.memberId()).list();
        list.forEach(item -> {
            item.getMore().setTdNum(Wx.MemberService.find().like(Member::getSourceInviteIds, item.getId()).count());
            item.getMore().setZtNum(Wx.MemberService.find().eq(Member::getSourceInviteIdL1, item.getId()).count());
        });
        return WxResult.success(list);
    }


    /**
     * 买节点
     */
    @PostMapping("/buyAgent")
    @WxRequestLog()
    public WxResult<Object> buyAgent(
            String hash
    ) {
        Wx.MemberService.buyAgent(Wx.memberId(), hash);
        return WxResult.success();
    }

    /**
     * 申请提现
     */
    @PostMapping("/withdraw")
    @WxRequestLog()
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
     *
     * @return
     */
    @PostMapping("/wallet/balance")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    public WxResult<List<PointWallet>> info(
            @ParamCheck PointWallet entity
    ) {
        IPage<PointWallet> page = pointWalletService.find().eq(PointWallet::getUid, Wx.memberId()).entity(entity).page();
        return WxResult.page(page);
    }

    /**
     * 代理详情
     */
    @PostMapping("/agent/info")
    @WxRequestLog()
    public WxResult<Object> agentInfo(
    ) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("max",Wx.SuperParamService.getInteger("MaxAgentNum",3000));
        map.put("finish",Wx.SuperParamService.getInteger("FinishAgentNum",0));
        return WxResult.success(map);
    }

    /**
     * 流水
     */
    @PostMapping("/money/record")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<MoneyRecord>> kuangRecord(
            MoneyRecord entity
    ){
        IPage<MoneyRecord> page = Wx.MoneyRecordService.find().entity(entity).eq(MoneyRecord::getUid,Wx.memberId()).page();
        return WxResult.page(page);
    }
}
