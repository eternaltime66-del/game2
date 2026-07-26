package org.wx.core.wxBusiness.account.entity.enums;

import com.alibaba.fastjson2.JSONObject;
import lombok.*;
import org.wx.core.wxBase.base.Wx;

import java.math.BigDecimal;

/**
 * 充值成功回调枚举（新玩法在此扩展）
 */
public enum RechargeCallbackEnum {

    USER_WEB3_PAY(
            "用户链上充值",
            UserWeb3PayDto.class,
            dto -> dto.init(),
            dto -> dto.error()
    ),

    ;

    private final String desc;
    @Getter
    public final Class<?> dtoClass;
    private final RechargeCallback<?> callback;
    private final ErrorCallback<?> errorCallback;

    <T> RechargeCallbackEnum(String desc, Class<T> dtoClass, RechargeCallback<T> callback, ErrorCallback<T> errorCallback) {
        this.desc = desc;
        this.dtoClass = dtoClass;
        this.callback = callback;
        this.errorCallback = errorCallback;
    }

    <T> RechargeCallbackEnum(String desc, Class<T> dtoClass, RechargeCallback<T> callback) {
        this.desc = desc;
        this.dtoClass = dtoClass;
        this.callback = callback;
        this.errorCallback = null;
    }

    @SuppressWarnings("unchecked")
    public void callBack(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return;
        }
        Object dto = JSONObject.parseObject(jsonStr, dtoClass);
        ((RechargeCallback<Object>) callback).apply(dto);
    }

    @SuppressWarnings("unchecked")
    public void callError(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return;
        }
        if (this.errorCallback == null) {
            throw new UnsupportedOperationException("当前枚举[" + this.name() + "]未定义错误回调");
        }
        Object dto = JSONObject.parseObject(jsonStr, dtoClass);
        ((ErrorCallback<Object>) this.errorCallback).apply(dto);
    }

    @Data
    public static class CommonCallbackDto {
        private String coinAddress;
        private String hash;
    }

    public interface RechargeCallback<T> {
        void apply(T dto);
    }

    public interface ErrorCallback<T> {
        void apply(T dto);
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class UserWeb3PayDto extends CommonCallbackDto {
        private String uid;
        private BigDecimal amount;

        public void init() {
            Wx.MoneyRecordService.changePoint(
                    uid,
                    MoneyDirectionType.Increase,
                    amount,
                    MoneyRecordType.WEB3_RECHARGE,
                    Wx.PointWalletService.getSysPointWallet(uid, PointCoin.USDT)
            );
        }

        public void error() {
            // 链上充值失败，无需额外回滚（订单状态已在调度中更新）
        }
    }
}
