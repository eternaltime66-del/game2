package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.wx.core.web3unit.Link;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBusiness.account.entity.enums.RechargeCallbackEnum;
import org.wx.core.wxBusiness.account.entity.enums.RechargeStateEnum;

import java.math.BigDecimal;

/**
 * Web3Recharge 实体类
 * @author 无心
 * @date 2026-01-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("app_web3_recharge")
public class Web3Recharge extends WxBaseEntity<Web3Recharge> {

    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 地址
     */
    private String fromAddress;

    /**
     * 地址
     */
    private String toAddress;

    /**
     * 地址
     */
    private Link chain;

    /**
     * 币种
     */
    private String coin;

    /**
     * 币种
     */
    private String coinAddress;


    /**
     * 状态
     */
    private RechargeStateEnum state;

    /**
     * uid
     */
    private String uid;

    /**
     * 手续费
     */
    private BigDecimal fee;

    /**
     * 到账金额
     */
    private BigDecimal realSend;

    /**
     * 商户id
     */
    private String merchantId;

    /**
     * 商户手续费
     */
    private BigDecimal merchantIdFee;

    /**
     * 交易哈希
     */
    private String hash;

    /**
     * 回调类型
     */
    private RechargeCallbackEnum callbackEnum;

    /**
     * 回调所需数据 Json字符串格式存储 使用时自动转换
     */
    private String callbackData;

    /**
     * 异常原因
     */
    private String errorMsg;

    public Web3Recharge() {

    }

    public Web3Recharge( String uid ,String fromAddress,String toAddress ,String hash,String coin  ,BigDecimal amount) {
        this.amount = amount;
        this.toAddress = toAddress;
        this.fromAddress = fromAddress;
        this.coin = coin;
        this.uid = uid;
        this.hash = hash;
        this.state = RechargeStateEnum.Wait;
    }

}
