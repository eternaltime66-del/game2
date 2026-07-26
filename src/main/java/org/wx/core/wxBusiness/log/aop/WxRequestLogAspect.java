package org.wx.core.wxBusiness.log.aop;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.unit.HttpServletUnit;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;
import org.wx.core.wxBusiness.log.entity.WxLogRequest;
import org.wx.core.wxBusiness.log.service.WxLogAsyncService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class WxRequestLogAspect {

    @Resource
    private WxLogAsyncService logAsyncService;

    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint pjp, WxRequestLog log) throws Throwable {
        HttpServletRequest request = HttpServletUnit.request();
        WxLogRequest record = new WxLogRequest();
        String requestId = WordUnit.nowId(4, 2);
        record.setId(requestId);
        record.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        record.setReqUrl(request != null ? request.getRequestURI() : null);
        record.setReqMethod(request != null ? request.getMethod() : null);
        record.setOperatorIp(request != null ? request.getRemoteAddr() : null);
        record.setStartTime(LocalDateTime.now());

        if (request != null && log.recordDataChange()) {
            request.setAttribute("ReqLogChange", true);
            request.setAttribute("ReqLogId", requestId);
        }
        if (request != null && log.recordRequest()) {
            record.setReqData(getMethodParamsToJson(pjp));
        }
        if (request != null) {
            try {
                Member member = Wx.member();
                record.setOperatorId(member.getId());
                record.setOperatorRole(member.getMemberRole().toString());
                if (log.recordDataChange()) {
                    request.setAttribute("ReqUserId", member.getId());
                }
            } catch (Exception ignored) {
            }
        }

        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            logAsyncService.finishRequestLog(record, result, error, log.recordResponse());
        }
    }

    private String getMethodParamsToJson(ProceedingJoinPoint pjp) {
        if (pjp == null) {
            return "{}";
        }
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = pjp.getArgs();
            Map<String, Object> paramMap = new HashMap<>();
            for (int i = 0; i < parameters.length; i++) {
                paramMap.put(parameters[i].getName(), args.length > i ? args[i] : null);
            }
            return JSONUtil.toJsonStr(paramMap);
        } catch (Exception e) {
            return "{\"error\":\"读取方法入参失败：" + e.getMessage() + "\"}";
        }
    }
}
