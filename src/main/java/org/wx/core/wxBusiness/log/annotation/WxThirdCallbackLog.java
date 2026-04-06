package org.wx.core.wxBusiness.log.annotation;

import java.lang.annotation.*;

/**
 * 三方回调日志注解
 * 用于标记需要记录三方回调请求日志的方法
 * @author 无心
 * @date 2026-01-19
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WxThirdCallbackLog {

    /**
     * 三方名称（如：微信支付、支付宝、银联等）
     */
    String thirdName();

    /**
     * 回调类型（支付/充值/确认/退款等）
     */
    String callbackType();
}