package org.wx.core.wxBusiness.account.service;

import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.account.entity.PointWallet;
import org.wx.core.wxBusiness.account.entity.enums.PointCoin;
import org.wx.core.wxBusiness.account.mapper.PointWalletMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * PointWallet Service实现类
 *
 * @author 无心
 * @date 2026-01-19
 */
@Service
public class PointWalletService extends WxServiceImpl<PointWalletMapper, PointWallet> {
    @Transactional(rollbackFor = Exception.class)
    public void init(String uid) {
        PointWallet pointWallet = new PointWallet();
        pointWallet.setUid(uid);
        pointWallet.setCoin(PointCoin.USDT);
        pointWallet.setBalance(BigDecimal.ZERO);
        pointWallet.setId(WordUnit.nowId(4, 1));
        this.save(pointWallet);
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid,coin")
    public PointWallet getSysPointWallet(String uid, PointCoin coin) {
        PointWallet wallet = this.find().eq(PointWallet::getUid, uid).eq(PointWallet::getCoin, coin).one();
        if (wallet == null) {
            return initSysPointWallet(uid, coin);
        }
        return wallet;
    }

    private PointWallet initSysPointWallet(String uid, PointCoin coin) {
        PointWallet pointWallet = new PointWallet();
        pointWallet.setUid(uid);
        pointWallet.setCoin(coin);
        pointWallet.setBalance(new BigDecimal("0"));
        pointWallet.setId(WordUnit.nowId(4, 1));
        this.save(pointWallet);
        return pointWallet;
    }






}
