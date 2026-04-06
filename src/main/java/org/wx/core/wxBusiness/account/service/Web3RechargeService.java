package org.wx.core.wxBusiness.account.service;

import com.alibaba.fastjson2.JSONObject;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.entity.Web3Recharge;
import org.wx.core.wxBusiness.account.entity.enums.RechargeCallbackEnum;
import org.wx.core.wxBusiness.account.entity.enums.RechargeStateEnum;
import org.wx.core.wxBusiness.account.mapper.Web3RechargeMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Web3Recharge Service实现类
 * @author 无心
 * @date 2026-01-19
 */
@Service
public class Web3RechargeService extends WxServiceImpl<Web3RechargeMapper, Web3Recharge> {


    /**
     * 创建充值订单（带泛型校验）
     */
    public <T> Web3Recharge createRechargeOrder(
            String uid,
            String fromAddress,
            String toAddress,
            BigDecimal amount,
            Web3Coin coin,
            String hash,
            RechargeCallbackEnum callbackEnum,
            T callbackData   // 泛型 DTO
    ) {

        // ① 泛型 DTO 校验（核心!!!）
        if (callbackEnum != null && callbackData != null) {
            Class<?> required = callbackEnum.getDtoClass();
            if (!required.isAssignableFrom(callbackData.getClass())) {
                throw new IllegalArgumentException(
                        "回调类型不匹配！期望: " + required.getSimpleName() +
                                "，实际: " + callbackData.getClass().getSimpleName()
                );
            }
        }

        // ③ 创建订单
        Web3Recharge recharge = new Web3Recharge()
                .setUid(uid)
                .setFromAddress(fromAddress)
                .setToAddress(toAddress)
                .setAmount(amount)
                .setCoinAddress(coin.getTokenAddress())
                .setCoin(coin.getCoinName())
                .setChain(coin.getChain())
                .setHash(hash)
                .setState(RechargeStateEnum.Wait)
                .setFee(BigDecimal.ZERO)
                .setRealSend(BigDecimal.ZERO)
                .setCallbackEnum(callbackEnum)
                .setCallbackData(
                        callbackData != null ? JSONObject.toJSONString(callbackData) : null
                );

        // ④ 写库
        this.save(recharge);
        return recharge;
    }
}
