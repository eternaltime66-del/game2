package org.wx.core.web3unit;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 统一链上转账记录 DTO
 */
@Data
public class Web3TransferRecord {

    /** IN / OUT */
    private String direction;

    /** 转出地址 */
    private String from;

    /** 转入地址 */
    private String to;

    /** 交易哈希 */
    private String hash;

    /** 金额（已处理精度） */
    private BigDecimal amount;

    /** 币种符号 */
    private String coin;

    /** 交易时间（毫秒时间戳） */
    private long txTime;

    /** 转出地址 */
    private String ddt;
    /** 转出地址 */
    private String coinAddress;
}
