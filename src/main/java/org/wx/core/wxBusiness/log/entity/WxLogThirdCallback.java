package org.wx.core.wxBusiness.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WxLogThirdCallback 实体类
 * @author 无心
 * @date 2026-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_log_third_callback")
public class WxLogThirdCallback extends WxBaseEntity<WxLogThirdCallback> {

    /**
     * 回调日志ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 链路ID（若能解析）
     */
    private String traceId;

    /**
     * 三方名称
     */
    private String thirdName;

    /**
     * 回调类型（支付/充值/确认等）
     */
    private String callbackType;

    /**
     * 我方回调地址
     */
    private String callbackUrl;

    /**
     * 三方请求/订单ID
     */
    private String thirdReqId;

    /**
     * 三方IP
     */
    private String reqIp;

    /**
     * 回调请求头 json格式化后的字符串
     */
    private String reqHeaders;

    /**
     * 回调原始数据 json格式化后的字符串
     */
    private String reqData;


    /**
     * 失败原因 抛出的异常原
     */
    private String errorMsg;

    /**
     * 收到回调时间
     */
    private LocalDateTime receiveTime;

    /**
     * 处理完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 处理耗时(ms)
     */
    private Integer useTimeMs;

}
