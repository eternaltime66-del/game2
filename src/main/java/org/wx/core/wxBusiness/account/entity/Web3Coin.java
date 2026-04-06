package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.web3unit.Link;
import org.wx.core.wxBase.base.WxBaseEntity;

import java.util.HashMap;

/**
 * Web3Coin 实体类
 * @author 无心
 * @date 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_web3_coin")
public class Web3Coin extends WxBaseEntity<Web3Coin> {

    public static Web3Coin BSC_BNB = new Web3Coin("BSC_BNB",Link.BSC,true,18,"BNB","BSC_BNB");
    public static Web3Coin BSC_USDT = new Web3Coin("BSC_USDT",Link.BSC,false,18,"USDT", "0x55d398326f99059ff775485246999027b3197955");
    public static Web3Coin BSC_DFS = new Web3Coin("BSC_DFS",Link.BSC,false,18,"DFS", "0xfF589C68C584d915a2eCb91A5e4BB2243EdD6aBC");

    public static HashMap<String,Web3Coin> map = new HashMap<>();

    static {
        map.put("BSC_BNB",BSC_BNB);
        map.put("BSC_USDT",BSC_USDT);
        map.put("BSC_DFS",BSC_DFS);
    }


    public Web3Coin() {
    }

    public Web3Coin(String coin, Link chain, Boolean main, Integer decimals, String coinName,String tokenAddress) {
        this.coin = coin;
        this.chain = chain;
        this.main = main;
        this.decimals = decimals;
        this.coinName = coinName;
        this.tokenAddress = tokenAddress;
    }

    /**
     * 币种id
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 币种全名
     */
    private String coin;

    /**
     * 链
     */
    private Link chain;

    /**
     * 是否主币
     */
    private Boolean main;

    public Boolean isMainCoin() {
        return main;
    }

    /**
     * 代币合约地址
     */
    private String tokenAddress;

    /**
     * 代币精度
     */
    private Integer decimals;

    /**
     * 是否为初始化所需币种
     */
    private Boolean isInit;

    /**
     * 币种名称
     */
    private String coinName;

    /**
     * 是否为初始化所需币种
     */
    private Integer sort;

}
