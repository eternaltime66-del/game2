package org.wx.core.wxBusiness.log.annotation;

import java.lang.annotation.*;

/**
 * 方法调用日志注解
 * 用于标记需要记录调用日志的方法
 * @author 无心
 * @date 2026-01-19
 */
@Target({ElementType.METHOD})  // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME)  // 运行时生效
@Documented
public @interface WxMethodLog {
    /**
     * 是否记录方法入参（默认记录）
     */
    boolean recordArgs() default true;

    /**
     * 是否记录方法返回值（默认记录）
     */
    boolean recordResult() default true;

    /**
     * 是否记录异常信息（默认记录）
     */
    boolean recordException() default true;
}