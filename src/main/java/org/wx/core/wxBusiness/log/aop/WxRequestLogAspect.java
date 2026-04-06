package org.wx.core.wxBusiness.log.aop;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
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
import org.wx.core.wxBusiness.log.service.WxLogRequestDetailService;
import org.wx.core.wxBusiness.log.service.WxLogRequestService;

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
    private  WxLogRequestService requestService;



    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint pjp, WxRequestLog log) throws Throwable {
        HttpServletRequest request = HttpServletUnit.request();
        WxLogRequest record = new WxLogRequest();
        String requestId = WordUnit.nowId(4,2);
        record.setId(requestId);
        record.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        record.setReqUrl(request != null ? request.getRequestURI() : null);
        record.setReqMethod(request != null ? request.getMethod() : null);
        record.setOperatorIp(request != null ? request.getRemoteAddr() : null);
        record.setReqState("RUNNING");
        record.setStartTime(LocalDateTime.now());

        requestService.save(record);
        Object result = null;
        Throwable error = null;
        if (request != null && log.recordDataChange()) {
            request.setAttribute("ReqLogChange",true);
            request.setAttribute("ReqLogId",requestId);
        }
        if (request!=null && log.recordRequest()){
            // 1. 获取URL参数并封装为JSON字符串
            record.setReqData(getMethodParamsToJson(pjp));
        }

        if (request!=null){
            // 1. 获取URL参数并封装为JSON字符串
            try {
                Member member = Wx.member();
                record.setOperatorId(member.getId());
                record.setOperatorRole(member.getMemberRole().toString());
            }catch (Exception e){

            }
        }


        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            // ===== 更新 L1 =====
            WxLogRequest update = new WxLogRequest();
            update.setId(record.getId());
            update.setFinishTime(LocalDateTime.now());
            update.setUseTimeMs(
                    (int) java.time.Duration.between(
                            record.getStartTime(),
                            update.getFinishTime()
                    ).toMillis()
            );
            if (log.recordResponse()){
                update.setResData(JSONObject.toJSONString(result));
            }
            if (error == null) {
                update.setReqState("SUCCESS");
            } else {
                update.setReqState("FAIL");
                update.setReqError(error.getMessage());
            }
            requestService.updateById(update);
        }
    }

    /**
     * 从ProceedingJoinPoint获取方法入参并转为JSON字符串
     * 支持：参数名+参数值、基本类型、自定义对象（如Wallet）、集合/数组
     */
    private String getMethodParamsToJson(ProceedingJoinPoint pjp) {
        if (pjp == null) {
            return "{}";
        }
        try {
            // 1. 获取方法签名和参数名
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            // 2. 获取方法入参值
            Object[] args = pjp.getArgs();
            // 3. 封装参数名-值映射
            Map<String, Object> paramMap = new HashMap<>();
            for (int i = 0; i < parameters.length; i++) {
                String paramName = parameters[i].getName(); // 获取参数名（如uid、wallet）
                Object paramValue = args.length > i ? args[i] : null;
                paramMap.put(paramName, paramValue);
            }
            // 4. 转为JSON字符串（自动处理自定义对象、集合等）
            return JSONUtil.toJsonStr(paramMap);
        } catch (Exception e) {
            return "{\"error\":\"读取方法入参失败：" + e.getMessage() + "\"}";
        }
    }
}