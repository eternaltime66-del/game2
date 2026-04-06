package org.wx.core.wxBusiness.log.aop;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wx.core.wxBase.unit.HttpServletUnit;
import org.wx.core.wxBusiness.log.annotation.WxThirdCallbackLog;
import org.wx.core.wxBusiness.log.entity.WxLogThirdCallback;
import org.wx.core.wxBusiness.log.service.WxLogThirdCallbackService;

import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 三方回调日志切面
 * 用于拦截标记了@WxThirdCallbackLog注解的方法，记录回调请求的完整日志
 * @author 无心
 * @date 2026-01-19
 */
@Slf4j
@Aspect
@Component
public class WxThirdCallbackLogAspect {

    @Resource
    private  WxLogThirdCallbackService thirdCallbackLogService;

    /**
     * 环绕通知，拦截标记了@WxThirdCallbackLog的方法
     */
    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint pjp, WxThirdCallbackLog log) throws Throwable {
        // 1. 获取HttpServletRequest对象
        HttpServletRequest request = HttpServletUnit.request();
        
        // 2. 初始化日志实体
        WxLogThirdCallback logEntity = new WxLogThirdCallback();
        
        // 3. 设置基础日志信息
        logEntity.setTraceId(UUID.randomUUID().toString().replace("-", "")); // 生成链路ID
        logEntity.setThirdName(log.thirdName()); // 从注解获取三方名称
        logEntity.setCallbackType(log.callbackType()); // 从注解获取回调类型
        logEntity.setReceiveTime(LocalDateTime.now()); // 记录收到回调的时间
        
        // 4. 从request中获取请求信息
        if (request != null) {
            logEntity.setCallbackUrl(request.getRequestURI()); // 我方回调地址
            logEntity.setReqIp(request.getRemoteAddr()); // 三方请求IP
            
            // 获取请求头并转为JSON字符串
            Map<String, String> headerMap = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headerMap.put(headerName, request.getHeader(headerName));
            }
            logEntity.setReqHeaders(JSON.toJSONString(headerMap));
            
            // 获取请求参数/请求体（这里根据实际场景调整，若为POST JSON则需读取流）
            // 注意：如果是POST请求且是JSON格式，需要通过流读取，建议封装到HttpServletUnit中
            String reqData = HttpServletUnit.getRequestBody(request); // 假设工具类有此方法
            if (StringUtils.hasText(reqData)) {
                logEntity.setReqData(reqData);
            } else {
                // 如果没有请求体，获取URL参数
                Map<String, String[]> paramMap = request.getParameterMap();
                logEntity.setReqData(JSON.toJSONString(paramMap));
            }
        }

        Object result = null;
        Throwable error = null;
        
        try {
            // 执行目标方法
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            // 捕获异常，记录错误信息
            error = t;
            throw t; // 继续抛出异常，不影响原有业务逻辑
        } finally {
            // 5. 补全日志信息并保存
            logEntity.setFinishTime(LocalDateTime.now()); // 处理完成时间
            
            // 计算处理耗时（毫秒）
            long useTime = java.time.Duration.between(
                    logEntity.getReceiveTime(),
                    logEntity.getFinishTime()
            ).toMillis();
            logEntity.setUseTimeMs((int) useTime);
            
            // 记录异常信息
            if (error != null) {
                logEntity.setErrorMsg(error.getMessage());
            }
            
            // 6. 保存日志到数据库
            try {
                thirdCallbackLogService.save(logEntity);
            } catch (Exception e) {
                // 日志保存失败不影响主业务
            }
        }
    }
}