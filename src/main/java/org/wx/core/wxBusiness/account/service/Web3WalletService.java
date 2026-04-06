package org.wx.core.wxBusiness.account.service;

import org.springframework.transaction.annotation.Transactional;

import org.wx.core.web3unit.Link;
import org.wx.core.web3unit.LinkGroup;
import org.wx.core.web3unit.Web3Tool;
import org.wx.core.web3unit.Web3WalletGenerator;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.entity.Web3Wallet;
import org.wx.core.wxBusiness.account.mapper.Web3WalletMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Web3Wallet Service实现类
 * @author 无心
 * @date 2026-01-19
 */
@Service
public class Web3WalletService extends WxServiceImpl<Web3WalletMapper, Web3Wallet> {

    public String initWeb3Wallet(String uid) {
        Web3WalletGenerator.WalletResult walletResult = Web3WalletGenerator.generateWallet(null);
        String mnemonic = walletResult.getMnemonic();
        Map<LinkGroup, Web3WalletGenerator.WalletInfo> wallet = walletResult.getWallet();
        List<Web3Coin> list = Wx.Web3CoinService.find().eq(Web3Coin::getIsInit, true).list();
        for (Web3Coin coin : list) {
            Web3WalletGenerator.WalletInfo walletInfo = wallet.get(
                    coin.getChain().getGroup()
            );
            String address = walletInfo.getAddress();
            String privateKey = walletInfo.getPrivateKey();
            Web3Wallet walletItem = Web3Wallet.defWeb3Wallet(
                    address,
                    privateKey,
                    uid,
                    coin
            );
            this.save(walletItem);
        }
        return mnemonic;
    }

    @Transactional(rollbackFor = Exception.class)
    public Web3Wallet getWeb3Wallet(String uid,Web3Coin coin) {
        return this.find().eq(Web3Wallet::getUid, uid).eq(Web3Wallet::getTokenAddress, coin.getTokenAddress()).one();
    }

    public Web3Wallet getWeb3AddressInfo(String uid, Link chain){
        Web3Wallet one = this.find().eq(Web3Wallet::getUid, uid).eq(Web3Wallet::getMain,true).eq(Web3Wallet::getChain, chain.toString()).one();
        if (one==null){
            ErrorFactory.throwError("用户未初始化钱包");
        }
        return one;
    }

    public String importWeb3Wallet(String uid,String tokenAddress) {
        Web3Coin web3Coin = Wx.Web3CoinService.importBscErc20Coin(tokenAddress);
        importWeb3Wallet(uid,web3Coin);
        return web3Coin.getCoin();
    }

    public void removeWeb3Wallet(String uid,String id) {
        Web3Wallet web3Wallet = this.getById(id);
        ErrorFactory.throwError(!web3Wallet.getUid().equals(uid),"非法操作-W01");
        this.removeById(id);
    }

    public void importWeb3Wallet(String uid,Web3Coin coin) {
        Web3Wallet one = this.find().eq(Web3Wallet::getUid, uid).eq(Web3Wallet::getTokenAddress, coin.getTokenAddress()).one();
        if (one==null){
            Web3Wallet mainInfo = getWeb3AddressInfo(uid, coin.getChain());
            Web3Wallet walletItem = Web3Wallet.defWeb3Wallet(
                    mainInfo.getAddress(),
                    mainInfo.prvkey(),
                    uid,
                    coin
            );
            this.save(walletItem);
        }
    }
    
}
