package org.wx.core.wxBase.factory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wx.core.web3unit.Web3HashCheckResult;
import org.wx.core.web3unit.Web3Tool;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBusiness.account.entity.Web3Recharge;
import org.wx.core.wxBusiness.account.entity.enums.RechargeCallbackEnum;
import org.wx.core.wxBusiness.account.entity.enums.RechargeStateEnum;
import org.wx.core.wxBusiness.account.service.Web3RechargeService;

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

    @Scheduled(cron = "0 * * * * ?")
    public void watchWeb3RechargeBefore() {
        log.warn("开始执行 watchWeb3RechargeBefore");

        System.out.println("开始执行 充值监控 - OverInit");
        rechargeService.forEachPage(
                new LambdaQueryWrapper<Web3Recharge>().eq(Web3Recharge::getState, RechargeStateEnum.Wait),
                recharge -> {
                    String hash = recharge.getHash();
                    Web3HashCheckResult result = new Web3HashCheckResult();
                    try {
                        result = Web3Tool.checkHash(
                                Wx.Web3CoinService.getByToken(recharge.getCoinAddress()),
                                hash,
                                recharge.getFromAddress(),
                                recharge.getToAddress(),
                                recharge.getAmount()
                        );
                    } catch (Exception e) {
                        result.setSuccess(false);
                        result.setFailMsg("hash检测异常");
                    }


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
                            if (callbackEnum != null) {
                                callbackEnum.callError(recharge.getCallbackData());
                            }
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
        log.warn("执行 watchWeb3RechargeBefore 完毕");
    }

}
