package org.wx.core.wxBase.aop;


import com.mysql.cj.util.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.HttpServletUnit;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 登录/权限校验：仅拦截方法上标注 @NeedHeader 的接口。
 */
@Aspect
@Component
public class NeedHeaderAop {

    @Pointcut("@annotation(org.wx.core.wxBase.annotation.NeedHeader)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        NeedHeader header = method.getAnnotation(NeedHeader.class);

        if (header.token()) {
            String token = HttpServletUnit.getHeader("token");
            ErrorFactory.throwError(StringUtils.isNullOrEmpty(token), "token不能为空");
        }
        if (header.roles().length > 0) {
            Member member = Wx.member();
            MemberRole[] roles = header.roles();
            MemberRole userRole = member.getMemberRole();
            ErrorFactory.throwError(!Arrays.asList(roles).contains(userRole), "权限异常 无法访问");
        }
        return pjp.proceed();
    }
}
