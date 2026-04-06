package org.wx.core.web3unit.evm;

import lombok.Data;
import java.math.BigInteger;

/**
 * TokenTraded 事件实体类
 * 对应合约中的 TokenTraded 事件字段
 */
@Data
public class EvmTokenTradedEvent {
    // 事件基础信息
    private String txHash;          // 交易哈希
    private String contractAddress; // 合约地址
    private long logIndex;          // 日志索引（区分同一个交易中的多个事件）
    
    // 事件字段
    private String user;            // indexed 索引字段
    private BigInteger inAmount;    // 转入金额
    private BigInteger outAmount;   // 转出金额
    private boolean isBuyWork;      // 是否购买作品
}