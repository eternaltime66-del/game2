package org.wx.core.wxBase.aop;

import io.netty.util.internal.StringUtil;
import lombok.SneakyThrows;
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
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.context.ReqContextHolder;
import org.wx.core.wxBase.exception.WxApiException;
import org.wx.core.wxBase.factory.ErrorFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 日志记录
 */
@Aspect
@Component
public class ParamCheckAop {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Pointcut(
            // 匹配所有标注 @RestController 的类 + 匹配所有标注 @Controller 的类
            value = "@within(org.springframework.web.bind.annotation.RestController) || " +
                    "@within(org.springframework.stereotype.Controller)"
    ) public void Pointcut() {
    }

    @Before("Pointcut()")
    public void doBefore(JoinPoint joinPoint) {
    }

    @Around("Pointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {

        ReqContextHolder.init();

        MethodSignature signature = ((MethodSignature) pjp.getSignature());
        //得到拦截的方法
        Method method = signature.getMethod();

        //获取方法参数注解，返回二维数组是因为某些参数可能存在多个注解
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations == null || parameterAnnotations.length == 0) {
            try {
                return pjp.proceed();
            }finally {
                ReqContextHolder.clear();
            }
        }

        //获取方法参数名
        String[] paramNames = signature.getParameterNames();

        //获取参数值
        Object[] paranValues = pjp.getArgs();

        //获取方法参数类型
        Class<?>[] parameterTypes = method.getParameterTypes();

        ServletRequestAttributes res = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                //如果该参数前面的注解是ParamCheck的实例，并且notNull()=true,则进行非空校验
                if (
                        parameterAnnotations[i][j] != null &&
                                parameterAnnotations[i][j] instanceof ParamCheck) {
                    paramsChecked(
                            paramNames[i],
                            paranValues[i],
                            parameterTypes[i] == null ? null : parameterTypes[i],
                            (ParamCheck) parameterAnnotations[i][j]
                    );
                    break;
                }
            }
        }
        try {
            return pjp.proceed();
        }finally {
            ReqContextHolder.clear();
        }
    }


    @SneakyThrows
    private void paramsChecked(String paramName, Object value, Class<?> parameterType, ParamCheck paramCheck) {
        //@ParamCheck String uid
        //paramName 参数名称  = > uid
        //value 你传进来的东西
        //parameterType  -> String
        //paramCheck @ParamCheck(p,m,code)

        //java.lang.String - > String
        String paramType = parameterType.getName().substring(parameterType.getName().lastIndexOf(".") + 1);
        //@ParamCheck(msg="用户uid",pattern="A|B",pattern_msg="") String uid
        //参数备注
        String msg = paramCheck.msg();
        if (StringUtil.isNullOrEmpty(msg)) {
            msg = paramName;
        }

        String finishMsg = String.format("数据类型 [ %s ] 的参数 [ %s ] 异常;异常原因: ", paramType, msg);

        if (paramCheck.notNull() && (value == null || StringUtil.isNullOrEmpty(value.toString()))) {
            ErrorFactory.throwError(true,"4401",finishMsg + "参数不能为空;");
//            throw new WxApiException(paramCheck.errCode(), );
        }

        String pattern = paramCheck.pattern();
        
        if (
                value != null && !StringUtil.isNullOrEmpty(value.toString()) && paramCheck.enumPattern()!=Object.class
        ){
            Class<?> aClass = paramCheck.enumPattern();
            ErrorFactory.throwError(!aClass.isEnum(),"{联系后台} : enums 过滤器 必须传入枚举类");
            Method values = aClass.getDeclaredMethod("values");
            Object[] invoke = (Object[]) values.invoke(null);
            String[] enumStrs = Arrays.stream(invoke)
                    .map(Object::toString)
                    .toArray(String[]::new);
            String enumPart = String.join("|", enumStrs);
            pattern += "|" + enumPart;
        }
        String patternMsg = paramCheck.patternMsg();

        if (
                value != null &&
                        !StringUtil.isNullOrEmpty(value.toString()) &&
                        !StringUtil.isNullOrEmpty(pattern) &&
                        !Pattern.matches(pattern, value.toString())
        ) {
            String errMsg = "参数不合法;";
            if (!StringUtil.isNullOrEmpty(patternMsg)) {
                errMsg += "请满足条件:[" + patternMsg + "]";
            }
            ErrorFactory.throwError(true,"4401", finishMsg + errMsg);
        }

        if (
                value != null &&
                !paramCheck.lessZero() &&
                Number.class.isAssignableFrom(parameterType) &&
                new BigDecimal(value.toString()).compareTo(BigDecimal.ZERO) < 0
        ) {
            ErrorFactory.throwError(true,"4401", finishMsg + "不可以小于0");
        }
        boolean assignableFrom = parameterType.isInstance(Number.class);

    }

}