package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBase.unit.AesUtil;
import org.wx.core.wxBase.unit.WordUnit;

import java.math.BigDecimal;

/**
 * Web3Wallet 实体类
 *
 * @author 无心
 * @date 2026-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_web3_wallet")
public class Web3Wallet extends WxBaseEntity<Web3Wallet> {

    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * uid
     */
    private String uid;

    /**
     * 链
     */
    private String chain;

    /**
     * 币种
     */
    private String coin;

    /**
     * 地址
     */
    private String address;

    /**
     * 地址
     */
    private String tokenAddress;

    /**
     * 地址
     */
    private Integer sort;

    private String privateKey;

    private Boolean main;

    /**
     * 余额
     */
    private BigDecimal balance;

    public static Web3Wallet defWeb3Wallet(
            String address,
            String privateKey,
            String uid,
            Web3Coin coin
    ) {
        Web3Wallet wallet = new Web3Wallet();
        String id = "W" + WordUnit.nowId(8, 1);
        wallet.setId(id);
        wallet.setUid(uid);
        wallet.setCoin(coin.getCoin());
        wallet.setAddress(address);
        wallet.setTokenAddress(coin.getTokenAddress());
        wallet.setMain(coin.isMainCoin());
        wallet.setChain(coin.getChain().toString());
        String encrypted = AesUtil.encrypt(privateKey, id);
        wallet.setPrivateKey(encrypted);
        wallet.setSort(coin.getSort());
        return wallet;
    }

    public String prvkey() {
        if (this.privateKey == null) {
            return null;
        }
        return AesUtil.decrypt(this.privateKey, this.id);
    }


}
