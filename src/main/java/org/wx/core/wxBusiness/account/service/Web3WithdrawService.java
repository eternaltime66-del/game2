package org.wx.core.wxBusiness.account.service;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.entity.Web3Withdraw;
import org.wx.core.wxBusiness.account.entity.enums.MoneyDirectionType;
import org.wx.core.wxBusiness.account.entity.enums.MoneyRecordType;
import org.wx.core.wxBusiness.account.entity.enums.PointCoin;
import org.wx.core.wxBusiness.account.mapper.Web3WithdrawMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Web3Withdraw Service实现类
 * @author 无心
 * @date 2026-03-12
 */
@Service
public class Web3WithdrawService extends WxServiceImpl<Web3WithdrawMapper, Web3Withdraw> {

    @RedisLock(key = "uid")
    @Transactional(rollbackFor = Exception.class)
    public Web3Withdraw submitWithdraw(String uid, BigDecimal amount) {
        Member member = Wx.MemberService.getById(uid);
        String orderId = Wx.MoneyRecordService.changePoint(
                uid,
                MoneyDirectionType.Reduce,
                amount,
                MoneyRecordType.SUB_WITHDRAW,
                Wx.PointWalletService.getSysPointWallet(uid, PointCoin.USDT)
        );
        JSONObject json = new JSONObject();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("提现金额错误");
        }

        Web3Withdraw withdraw = new Web3Withdraw();

        withdraw.setUid(uid);
        withdraw.setAmount(amount);

        // 手动写币种
        withdraw.setCoin("USDT");
        withdraw.setCoinAddress(Web3Coin.BSC_USDT.getTokenAddress());
        withdraw.setToAddress(member.getAddress());
        // 状态
        withdraw.setState("待审核");

        // 手续费（可以自己算）
        withdraw.setFee(BigDecimal.ZERO);

        // 实际到账
        withdraw.setRealSend(amount.toPlainString());

        // 链
        withdraw.setChain("BSC");

        this.save(withdraw);
        Wx.MoneyRecordService.setMoreData(orderId,json);
        return withdraw;

    }


    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "id")
    public void success(String id,String hash) {
        Web3Withdraw w = this.getById(id);
        ErrorFactory.throwError(!w.getState().equals("待审核"),"状态有误");
        w.setHash(hash);
        w.setState("已通过");
        Wx.MoneyRecordService.entityWeb3(
                w.getUid(),
                MoneyDirectionType.Increase,
                w.getAmount(),
                MoneyRecordType.SUCCESS_WITHDRAW,
                Web3Coin.BSC_USDT,
                hash
        );
        this.wxUpdateById(w);
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "id")
    public void fail(String id,String hash) {
        Web3Withdraw w = this.getById(id);
        ErrorFactory.throwError(!w.getState().equals("待审核"),"状态有误");
        w.setHash(hash);
        w.setState("已拒绝");
        Wx.MoneyRecordService.changePoint(
                w.getUid(),
                MoneyDirectionType.Increase,
                w.getAmount(),
                MoneyRecordType.FAIL_WITHDRAW,
                Wx.PointWalletService.getSysPointWallet(w.getUid(), PointCoin.USDT)
        );
        this.wxUpdateById(w);
    }
}
