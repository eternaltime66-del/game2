package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Web3Withdraw 实体类
 * @author 无心
 * @date 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_web3_withdraw")
public class Web3Withdraw extends WxBaseEntity<Web3Withdraw> {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private BigDecimal amount;

    /**
     * 目标地址
     */
    private String toAddress;

    /**
     * 状态
     *     待审核 / 已通过 / 已拒绝
     */
    private String state;

    private String uid;

    private String hash;

    /**
     * 链类型 Link
     */
    private String chain;

    /**
     * 手续费
     */
    private BigDecimal fee;

    /**
     * 实际到账
     */
    private String realSend;

    /**
     * 币种 Web3Coin
     */
    private String coin;

    private String coinAddress;

}
