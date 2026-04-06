package org.wx.core.web3unit;

import lombok.Data;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Web3Config - 改为完全支持 read-only 模式
 */
@Data
public class Web3Config {

    private String privateKey;   // 可为 null（read-only 模式）
    private Web3j web3j;
    private String nodeUrl;
    private long chainId;
    private Credentials credentials;
    // ============================================
    // BSC 主网
    // ============================================
    public static Web3Config createBsc(String privateKey) {
        Web3Config cfg = new Web3Config();
        cfg.privateKey = (privateKey == null || privateKey.isEmpty()) ? null : privateKey;
        cfg.chainId = 56;
        cfg.nodeUrl = "https://bsc-dataseed1.binance.org/";
        cfg.web3j = Web3j.build(new HttpService(cfg.nodeUrl));
        return cfg;
    }

    // ============================================
    // ETH 主网
    // ============================================
    public static Web3Config createEth(String privateKey) {
        Web3Config cfg = new Web3Config();
        cfg.privateKey = (privateKey == null || privateKey.isEmpty()) ? null : privateKey;
        cfg.chainId = 1;
        cfg.nodeUrl = "https://eth-mainnet.public.blastapi.io";  // 使用 Infura 节点服务
        cfg.web3j = Web3j.build(new HttpService(cfg.nodeUrl));
        return cfg;
    }
}
