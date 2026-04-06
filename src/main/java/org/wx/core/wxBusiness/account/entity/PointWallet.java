package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBusiness.account.entity.enums.PointCoin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PointWallet 实体类
 * @author 无心
 * @date 2026-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_point_wallet")
public class PointWallet extends WxBaseEntity<PointWallet> {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private String id;


    /**
     * 币种
     */
    private PointCoin coin;

    /**
     * 可用余额
     */
    private BigDecimal balance;

    /**
     * 用户uid
     */
    private String uid;

}
