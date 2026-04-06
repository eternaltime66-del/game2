package org.wx.core.wxBase.aop;


import com.mysql.cj.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.HttpServletUnit;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;


import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 日志记录
 */
@Aspect
@Component
public class NeedHeaderAop {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Pointcut(value = "execution(* org.wx.core.*.*.controller..*.*(..))")
    public void Pointcut() {
    }

    @Before("Pointcut()")
    public void doBefore(JoinPoint joinPoint) {
    }

    @Around("Pointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = ((MethodSignature) pjp.getSignature());
        //得到拦截的方法
        Method method = signature.getMethod();

        //判断方法上是否有NeedHeader 注解
        if(method.isAnnotationPresent(NeedHeader.class)){
            NeedHeader header = method.getAnnotation(NeedHeader.class);
            if(header.token()){
                String token = HttpServletUnit.getHeader("token");
                ErrorFactory.throwError(StringUtils.isNullOrEmpty(token),"token不能为空");
            }
            if (header.roles().length>0){
                Member member = Wx.member();
                MemberRole[] roles = header.roles();
                MemberRole userRole = member.getMemberRole();
                ErrorFactory.throwError(!Arrays.asList(roles).contains(userRole),"权限异常 无法访问");
            }
        };
        return pjp.proceed();
    }


    public static void main(String[] args) {
        StringBuffer a = new StringBuffer("a");
        toDo(a);
//        //System.err.println(a);
    }

    public static void toDo(StringBuffer xx){
        xx.append("b");
    }
}