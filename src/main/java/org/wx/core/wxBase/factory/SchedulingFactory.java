package org.wx.core.wxBase.factory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wx.core.web3unit.Link;
import org.wx.core.web3unit.Web3Config;
import org.wx.core.web3unit.Web3HashCheckResult;
import org.wx.core.web3unit.Web3Tool;
import org.wx.core.web3unit.evm.EvmFun;
import org.wx.core.web3unit.evm.EvmHashResult;
import org.wx.core.web3unit.evm.EvmUnit;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.unit.ListUnit;
import org.wx.core.wxBusiness.account.entity.Web3Recharge;
import org.wx.core.wxBusiness.account.entity.Web3RunWatch;
import org.wx.core.wxBusiness.account.entity.Web3Wallet;
import org.wx.core.wxBusiness.account.entity.enums.RechargeCallbackEnum;
import org.wx.core.wxBusiness.account.entity.enums.RechargeStateEnum;
import org.wx.core.wxBusiness.account.service.Web3RechargeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * @author 无心
 * @date 2021/8/13
 * @msg 备注 计时器工厂
 */

@Slf4j
@Service("SchedulingFactory")
public class SchedulingFactory {

    @Resource
    public Web3RechargeService rechargeService;



    @Scheduled(cron = "0/10 * * * * ?")
    public void watchWeb3RechargeBefore() {
        if (!Wx.INIT) {
            return;
        }
        rechargeService.forEachPage(
                new LambdaQueryWrapper<Web3Recharge>().eq(Web3Recharge::getState, RechargeStateEnum.Wait),
                recharge -> {
                    String hash = recharge.getHash();
                    Web3HashCheckResult result =
                            Web3Tool.checkHash(
                                    Wx.Web3CoinService.getByToken(recharge.getCoinAddress()),
                                    hash,
                                    recharge.getFromAddress(),
                                    recharge.getToAddress(),
                                    recharge.getAmount()
                            );

                    if (result.isSuccess()) {
                        recharge.setState(RechargeStateEnum.Success);
                        System.out.println("开始执行回调");
                        RechargeCallbackEnum callbackEnum = recharge.getCallbackEnum();
                        if (callbackEnum != null) {
                            callbackEnum.callBack(recharge.getCallbackData());

                        }
                    } else {
                        RechargeCallbackEnum callbackEnum = recharge.getCallbackEnum();
                        Integer retry = Wx.RedisFactory.get(hash, Integer.class);
                        retry = (retry == null ? 1 : retry + 1);
                        if (retry > 60) {
                            recharge.setState(RechargeStateEnum.Fail);
                            recharge.setErrorMsg(result.getFailMsg());
                            callbackEnum.callError(recharge.getCallbackData());
                        } else {
                            Wx.RedisFactory.setBuyHour(hash, retry, 6L);
                            recharge.setState(RechargeStateEnum.Wait);
                        }
                    }
                    // 只更新必要字段
                    rechargeService.wxUpdateById(
                            recharge,
                            Web3Recharge::getState,
                            Web3Recharge::getErrorMsg
                    );
                }
        );

    }


}