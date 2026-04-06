package org.wx.core.wxBusiness.account.service;

import org.springframework.transaction.annotation.Transactional;
import org.wx.core.web3unit.ERC20Contract;
import org.wx.core.web3unit.Link;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.mapper.Web3CoinMapper;
import org.springframework.stereotype.Service;

/**
 * Web3Coin Service实现类
 * @author 无心
 * @date 2026-01-20
 */
@Service
public class Web3CoinService extends WxServiceImpl<Web3CoinMapper, Web3Coin> {

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "tokenAddress")
    public Web3Coin importBscErc20Coin(String tokenAddress) {
        String coinSymbol = ERC20Contract.at(tokenAddress).read().symbol.val();
        Integer decimals = ERC20Contract.at(tokenAddress).read().decimals.val();
        ErrorFactory.notEmpty(coinSymbol,"代币地址无效 审核失败");
        String coinName = "BSC_"+coinSymbol;
        Web3Coin web3Coin = this.getByToken(tokenAddress);
        if (web3Coin==null){
            web3Coin = new Web3Coin();
            web3Coin.setId(tokenAddress);
            web3Coin.setCoin(coinName);
            web3Coin.setChain(Link.BSC);
            web3Coin.setMain(false);
            web3Coin.setTokenAddress(tokenAddress);
            web3Coin.setDecimals(decimals);
            web3Coin.setCoinName(coinSymbol);
            web3Coin.setSort(999);
            this.save(web3Coin);
        }
        return web3Coin;
    }

    @Transactional(rollbackFor = Exception.class)
    public Web3Coin getByToken(String tokenAddress) {
        return this.find().eq(Web3Coin::getTokenAddress,tokenAddress).one();
    }
}
