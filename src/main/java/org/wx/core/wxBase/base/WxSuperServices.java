package org.wx.core.wxBase.base;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.wx.core.wxBase.factory.CodeFactory;
import org.wx.core.wxBase.factory.RedisFactory;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.service.*;
import org.wx.core.wxBusiness.common.service.WxMoreLangService;
import org.wx.core.wxBusiness.common.service.WxSuperParamService;
import org.wx.core.wxBusiness.log.service.WxLogRequestDetailService;
import org.wx.core.wxBusiness.log.service.WxLogThirdPartyService;

@Component
@Getter
public class WxSuperServices {

    @Resource
    private MemberService memberService;
    @Resource
    private RedisFactory redisFactory;
    @Resource
    private WxLogThirdPartyService wxLogThirdPartyService;
    @Resource
    private WxLogRequestDetailService wxLogRequestDetailService;
    @Resource
    private Web3WalletService web3WalletService;
    @Resource
    private PointWalletService pointWalletService;
    @Resource
    private Web3CoinService web3CoinService;
    @Resource
    private MoneyRecordService moneyRecordService;
    @Resource
    private Web3RunWatchService web3RunWatchService;
    @Resource
    private WxMoreLangService wxMoreLangService;
    @Resource
    private WxSuperParamService wxSuperParamService;
    @PostConstruct
    public void init() {
        Wx.init(this);
    }
}
