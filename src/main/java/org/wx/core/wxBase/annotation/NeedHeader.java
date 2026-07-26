package org.wx.core.wxBase.annotation;


import org.wx.core.wxBusiness.account.entity.enums.MemberRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
*@author 无心
*@date ${DATE}
*@msg 备注
*@demo ${NAME}
*/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NeedHeader {
    /** 是否需要登录 token */
    boolean token() default true;

    /** 是否校验用户冻结状态 */
    boolean frozen() default true;

    /** 是否开启 IP 单接口限流 */
    boolean ipRateLimit() default false;

    /** IP 限流：每分钟最大次数 */
    int ipLimitCount() default 60;

    /** 用户权限（可选，不传表示不限制） */
    MemberRole[] roles() default {};
}
