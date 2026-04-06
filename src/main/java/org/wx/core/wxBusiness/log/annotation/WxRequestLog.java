package org.wx.core.wxBusiness.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WxRequestLog {

    /** 是否记录请求入参 */
    boolean recordRequest() default true;

    /** 是否记录响应数据 */
    boolean recordResponse() default true;

    /** 是否记录数据变更（L2） */
    boolean recordDataChange() default false;

    /** 模块名 */
    String module() default "";

    /** 动作描述 */
    String action() default "";
}
