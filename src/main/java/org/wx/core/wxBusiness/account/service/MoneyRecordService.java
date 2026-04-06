package org.wx.core.wxBusiness.account.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.*;
import org.wx.core.wxBusiness.account.entity.enums.MoneyDirectionType;
import org.wx.core.wxBusiness.account.entity.enums.MoneyRecordType;
import org.wx.core.wxBusiness.account.entity.enums.PointCoin;
import org.wx.core.wxBusiness.account.mapper.MoneyRecordMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * MoneyRecord Service实现类
 *
 * @author 无心
 * @date 2026-01-21
 */
@Service
public class MoneyRecordService extends WxServiceImpl<MoneyRecordMapper, MoneyRecord> {


    /**
     * Web3钱包金额变动（核心逻辑：记录流水+关联链上Hash，适配Web3Wallet）
     */
    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid") // 锁粒度：用户+Web3钱包ID
    public String entityWeb3(
            String uid,
            MoneyDirectionType direction,
            BigDecimal amount,
            MoneyRecordType type,
            Web3Coin coin,
            String hash
    ){
        Member member = Wx.MemberService.getById(uid);
        // 1. 核心校验（仅保留必选）
//        Web3Wallet wallet = Wx.Web3WalletService.getWeb3Wallet(uid, coin);
//        ErrorFactory.notNull(wallet, "Web3钱包无效");
//        ErrorFactory.notEquals(wallet.getUid(), uid, "用户与Web3钱包不匹配");
        ErrorFactory.notNull(hash, "链上交易Hash不能为空");
        ErrorFactory.notNull(amount, "变动金额不能为空");

        // 3. 构建流水记录（适配MoneyRecord+Web3Wallet字段）
        MoneyRecord moneyRecord = MoneyRecord.web3(
                uid,
                type,
                amount,
                direction
        );
        // 填充Web3专属字段
        moneyRecord.setWebAmount(amount);   // 链上变动金额
        moneyRecord.setHash(hash);          // 链上交易Hash
        moneyRecord.setCoin(coin.getCoin()); // Web3钱包币种
//        moneyRecord.setWalletId(wallet.getId()); // Web3钱包ID
        moneyRecord.setWalletType("Web3");  // 标记钱包类型为Web3
        moneyRecord.setAddress(member.getAddress());
        // 4. 保存流水+更新Web3钱包余额
        this.save(moneyRecord);
        return moneyRecord.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid,wallet.id")
    public String changePoint(
            String uid,
            MoneyDirectionType direction,
            BigDecimal amount,
            MoneyRecordType type,
            PointWallet wallet
    ) {
        PointCoin coin = wallet.getCoin();
        wallet = Wx.PointWalletService.getById(wallet.getId());
        Member member = Wx.MemberService.getById(uid);
        if (member != null) {
            ErrorFactory.notEquals(wallet.getUid(), uid, "用户与钱包不匹配");
            BigDecimal balance = wallet.getBalance();
            BigDecimal after = BigDecimal.ZERO;
            if (direction.equals(MoneyDirectionType.Increase)) {
                amount = amount.setScale(coin.decimals, RoundingMode.DOWN);
                after = balance.add(amount);
            }
            if (direction.equals(MoneyDirectionType.Reduce)) {
                amount = amount.setScale(coin.decimals, RoundingMode.UP);
                after = balance.subtract(amount);
                ErrorFactory.throwError(after.compareTo(BigDecimal.ZERO) < 0, "余额不足");
            }
            MoneyRecord moneyRecord = MoneyRecord.point(uid, type, balance, after, amount, direction, wallet);
            this.save(moneyRecord);
            moneyRecord.setAddress(member.getAddress());
            wallet.setBalance(after);
            Wx.PointWalletService.wxUpdateById(wallet,PointWallet::getBalance);
            return moneyRecord.getId();
        } else {
            return "Error Uid";
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setMoreData(String recordId, Object data) {
        LambdaUpdateWrapper<MoneyRecord> wrapper = updateWrapper()
                .eq(MoneyRecord::getId, recordId)
                .set(MoneyRecord::getMore, JSONObject.toJSONString(data));
        this.update(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void findHash(String hash) {
        long count = this.find().like(MoneyRecord::getMore, hash.toLowerCase()).count();
        ErrorFactory.throwError(count>0,"哈希无效-重复");
    }
}
