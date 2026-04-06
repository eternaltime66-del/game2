package org.wx.core.wxBusiness.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WxLogMethod 实体类
 * @author 无心
 * @date 2026-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_log_method")
public class WxLogMethod extends WxBaseEntity<WxLogMethod> {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 链路ID
     */
    private String traceId;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 方法入参
     */
    private Object methodArgs;

    /**
     * 方法返回值
     */
    private Object methodResult;

    /**
     * 异常信息
     */
    private String exceptionMsg;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private Integer useTimeMs;

}
