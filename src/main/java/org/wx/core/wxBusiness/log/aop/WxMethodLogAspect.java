package org.wx.core.wxBusiness.log.aop;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBusiness.log.annotation.WxMethodLog;
import org.wx.core.wxBusiness.log.entity.WxLogMethod;
import org.wx.core.wxBusiness.log.service.WxLogMethodService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 方法调用日志切面
 * 拦截标记@WxMethodLog的方法，自动记录方法调用全量日志
 * @author 无心
 * @date 2026-01-19
 */
@Aspect
@Component
public class WxMethodLogAspect {

    @Resource
    public WxLogMethodService wxLogMethodService;

    /**
     * 环绕通知：拦截所有标记@WxMethodLog的方法
     */
    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, WxMethodLog log) throws Throwable {
        // 1. 生成唯一链路ID
        String traceId = UUID.randomUUID().toString().replace("-", "");
        
        // 2. 初始化日志实体
        WxLogMethod logEntity = new WxLogMethod();
        logEntity.setTraceId(traceId);
        logEntity.setStartTime(LocalDateTime.now());

        // 3. 获取方法基础信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 填充类名和方法名
        logEntity.setClassName(joinPoint.getTarget().getClass().getName());
        logEntity.setMethodName(method.getName());

        // 4. 记录方法入参（根据注解配置）
        if (log.recordArgs()) {
            Object[] args = joinPoint.getArgs();
            try {
                // 序列化入参为JSON字符串（避免Object类型存储问题）
                logEntity.setMethodArgs(JSON.toJSONString(args));

            } catch (Exception e) {
                logEntity.setMethodArgs("入参序列化失败：" + e.getMessage());
            }
        } else {
            logEntity.setMethodArgs("未记录入参");
        }

        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            // 6. 捕获方法执行异常
            exception = t;
            throw t; // 继续抛出异常，不影响原有业务逻辑
        } finally {
            // 7. 补全日志信息
            logEntity.setFinishTime(LocalDateTime.now());
            // 计算方法执行耗时（毫秒）
            long useTime = java.time.Duration.between(
                    logEntity.getStartTime(),
                    logEntity.getFinishTime()
            ).toMillis();
            logEntity.setUseTimeMs((int) useTime);

            // 8. 记录方法返回值（根据注解配置）
            if (log.recordResult() && exception == null) {
                try {
                    logEntity.setMethodResult(result != null ? JSON.toJSONString(result) : "null");
                } catch (Exception e) {
                           logEntity.setMethodResult("返回值序列化失败：" + e.getMessage());
                }
            } else if (exception != null) {
                logEntity.setMethodResult("方法执行异常，无返回值");
            } else {
                logEntity.setMethodResult("未记录返回值");
            }

            // 9. 记录异常信息（根据注解配置）
            if (log.recordException() && exception != null) {
                // 拼接异常信息（包含异常类型+消息+堆栈摘要）
                StringBuilder exceptionMsg = new StringBuilder();
                exceptionMsg.append(exception.getClass().getName()).append(": ").append(exception.getMessage());
                // 可选：添加堆栈前5行，便于排查
                StackTraceElement[] stackTrace = exception.getStackTrace();
                if (stackTrace.length > 0) {
                    exceptionMsg.append("\n堆栈摘要：");
                    for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                        exceptionMsg.append("\n\t").append(stackTrace[i].toString());
                    }
                }
                logEntity.setExceptionMsg(exceptionMsg.toString());
            } else if (exception != null) {
                logEntity.setExceptionMsg("未记录异常信息");
            } else {
                logEntity.setExceptionMsg("无异常");
            }

            // 10. 保存日志到数据库
            try {
                wxLogMethodService.save(logEntity);
            } catch (Exception e) {

            }
        }
    }
}