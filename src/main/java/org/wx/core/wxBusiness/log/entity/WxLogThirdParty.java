package org.wx.core.wxBusiness.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WxLogThirdParty 实体类
 * @author 无心
 * @date 2026-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_log_third_party")
public class WxLogThirdParty extends WxBaseEntity<WxLogThirdParty> {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 链路ID
     */
    private String traceId;

    /**
     * 三方名称
     */
    private String thirdName;

    private String reqUrl;

    private String reqMethod;

    /**
     * 请求参数
     */
    private String reqData;

    /**
     * 返回数据
     */
    private String resData;

    /**
     * SUCCESS / FAIL
     */
    private String reqState;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private Integer useTimeMs;

}
