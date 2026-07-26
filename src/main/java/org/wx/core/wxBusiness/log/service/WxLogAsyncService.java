package org.wx.core.wxBusiness.log.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSONObject;
import org.wx.core.wxBusiness.log.entity.WxLogMethod;
import org.wx.core.wxBusiness.log.entity.WxLogRequest;
import org.wx.core.wxBusiness.log.entity.WxLogRequestDetail;
import org.wx.core.wxBusiness.log.entity.WxLogThirdCallback;
import org.wx.core.wxBusiness.log.entity.WxLogThirdParty;

import java.time.LocalDateTime;

/**
 * 日志异步落库，所有 DB 写入走独立线程池，不阻塞业务请求线程。
 */
@Slf4j
@Service
public class WxLogAsyncService {

    @Resource
    private WxLogRequestService requestService;
    @Resource
    private WxLogRequestDetailService detailService;
    @Resource
    private WxLogThirdCallbackService thirdCallbackService;
    @Resource
    private WxLogMethodService methodService;
    @Resource
    private WxLogThirdPartyService thirdPartyService;

    @Async("logTaskExecutor")
    public void finishRequestLog(WxLogRequest record, Object result, Throwable error, boolean recordResponse) {
        try {
            LocalDateTime finishTime = LocalDateTime.now();
            record.setFinishTime(finishTime);
            record.setUseTimeMs((int) java.time.Duration.between(record.getStartTime(), finishTime).toMillis());
            record.setReqState(error == null ? "SUCCESS" : "FAIL");
            if (recordResponse) {
                record.setResData(JSONObject.toJSONString(result));
            }
            if (error != null) {
                record.setReqError(error.getMessage());
            }
            requestService.save(record);
        } catch (Exception e) {
            log.error("异步保存请求日志失败, id={}", record.getId(), e);
        }
    }

    @Async("logTaskExecutor")
    public void saveRequestDetail(WxLogRequestDetail detail) {
        try {
            detailService.save(detail);
        } catch (Exception e) {
            log.error("异步保存变更日志失败, requestId={}", detail.getRequestId(), e);
        }
    }

    @Async("logTaskExecutor")
    public void saveThirdCallback(WxLogThirdCallback entity) {
        try {
            thirdCallbackService.save(entity);
        } catch (Exception e) {
            log.error("异步保存三方回调日志失败", e);
        }
    }

    @Async("logTaskExecutor")
    public void saveMethodLog(WxLogMethod entity) {
        try {
            methodService.save(entity);
        } catch (Exception e) {
            log.error("异步保存方法日志失败", e);
        }
    }

    @Async("logTaskExecutor")
    public void saveThirdParty(WxLogThirdParty entity) {
        try {
            thirdPartyService.save(entity);
        } catch (Exception e) {
            log.error("异步保存三方请求日志失败, traceId={}", entity.getTraceId(), e);
        }
    }
}
