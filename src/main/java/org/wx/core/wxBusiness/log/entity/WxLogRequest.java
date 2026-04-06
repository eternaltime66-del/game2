package org.wx.core.wxBusiness.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WxLogRequest 实体类
 * @author 无心
 * @date 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_log_request")
public class WxLogRequest extends WxBaseEntity<WxLogRequest> {

    /**
     * 请求日志ID
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 用户ID（可能为空）
     */
    private String operatorId;

    /**
     * 用户角色
     */
    private String operatorRole;

    /**
     * 用户类型
     */
    private String operatorType;

    /**
     * 客户端IP
     */
    private String operatorIp;

    /**
     * 请求URL
     */
    private String reqUrl;

    /**
     * HTTP方法
     */
    private String reqMethod;

    /**
     * 请求参数
     */
    private String reqData;

    /**
     * 返回数据（可裁剪）
     */
    private String resData;

    /**
     * SUCCESS / FAIL
     */
    private String reqState;

    /**
     * 错误信息
     */
    private String reqError;

    /**
     * 请求开始时间
     */
    private LocalDateTime startTime;

    /**
     * 请求结束时间
     */
    private LocalDateTime finishTime;

    /**
     * 耗时(ms)
     */
    private Integer useTimeMs;

}
