package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBusiness.account.entity.enums.PointOrderState;
import org.wx.core.wxBusiness.account.entity.enums.PointOrderType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PointPayOrder 实体类
 * @author 无心
 * @date 2026-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_point_pay_order")
public class PointPayOrder extends WxBaseEntity<PointPayOrder> {

    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 状态
     */
    private PointOrderState state;
    /**
     * uid
     */
    private String uid;
    /**
     * amount
     */
    private BigDecimal amount;
    /**
     * 订单类型
     */
    private PointOrderType type;
    /**
     * 更多参数
     */
    private String more;

}
