package org.wx.core.wxBusiness.account.entity.enums;

import com.alibaba.fastjson2.JSONObject;
import lombok.*;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.unit.HttpRequestUnit;

import java.math.BigDecimal;
import java.util.Map;

public enum RechargeCallbackEnum {

    ;
    /**
     * 描述
     */
    private final String desc;
    /**
     * DTO 类型
     */
    @Getter
    public final Class<?> dtoClass;
    /**
     * 正常回调执行体（原变量，保留）
     */
    private final RechargeCallback<?> callback;
    /**
     * 新增：错误回调执行体（专门存储错误回调，解决类型混淆问题）
     */
    private final ErrorCallback<?> errorCallback;

    // 修正1：带错误回调的构造方法，新增errorCallback参数赋值
    <T> RechargeCallbackEnum(String desc, Class<T> dtoClass, RechargeCallback<T> callback, ErrorCallback<T> errorCallback) {
        this.desc = desc;
        this.dtoClass = dtoClass;
        this.callback = callback;
        this.errorCallback = errorCallback; // 存储错误回调，不再浪费参数
    }

    // 修正2：无错误回调的构造方法，给errorCallback赋值null
    <T> RechargeCallbackEnum(String desc, Class<T> dtoClass, RechargeCallback<T> callback) {
        this.desc = desc;
        this.dtoClass = dtoClass;
        this.callback = callback;
        this.errorCallback = null; // 无错误回调时置空
    }

    /**
     * 自动 JSON → DTO → 执行正常回调
     */
    @SuppressWarnings("unchecked")
    public void callBack(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return;
        }
        Object dto = JSONObject.parseObject(jsonStr, dtoClass);
        ((RechargeCallback<Object>) callback).apply(dto);
    }

    /**
     * 修正3：执行错误回调，使用专门的errorCallback变量，避免强转
     */
    @SuppressWarnings("unchecked")
    public void callError(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return;
        }
        // 先判断错误回调是否存在，避免空指针
        if (this.errorCallback == null) {
            throw new UnsupportedOperationException("当前枚举[" + this.name() + "]未定义错误回调");
            // 或选择静默处理：return;
        }
        Object dto = JSONObject.parseObject(jsonStr, dtoClass);
        // 直接使用errorCallback，无需强转callback（核心修复点）
        ((ErrorCallback<Object>) this.errorCallback).apply(dto);
    }

    @Data
    public static class CommonCallbackDto {
        private String coinAddress;
        private String hash;

        public void error() {

        }
    }


    // 函数式接口：正常回调
    public interface RechargeCallback<T> {
        void apply(T dto);
    }

    // 函数式接口：错误回调
    public interface ErrorCallback<T> {
        void apply(T dto);
    }

}