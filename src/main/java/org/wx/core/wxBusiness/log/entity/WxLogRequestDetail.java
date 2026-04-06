package org.wx.core.wxBusiness.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WxLogRequestDetail 实体类
 * @author 无心
 * @date 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_log_request_detail")
public class WxLogRequestDetail extends WxBaseEntity<WxLogRequestDetail> {

    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 关联L1请求ID
     */
    private String requestId;

    /**
     * 操作表名
     */
    private String tableName;

    /**
     * INSERT / UPDATE / DELETE
     */
    private String actionType;

    /**
     * 变更前数据
     */
    private String beforeData;

    /**
     * 变更后数据
     */
    private String afterData;

    /**
     * 变动的数据
     */
    private String changeData;

    /**
     * 操作人ID
     */
    private String operatorId;

}
