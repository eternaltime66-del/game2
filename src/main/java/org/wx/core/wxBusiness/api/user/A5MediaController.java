package org.wx.core.wxBusiness.api.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.web3unit.Link;
import org.wx.core.web3unit.Web3Tool;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.entity.Web3Wallet;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.account.service.Web3RechargeService;
import org.wx.core.wxBusiness.common.entity.Article;
import org.wx.core.wxBusiness.common.entity.Banner;
import org.wx.core.wxBusiness.common.service.ArticleService;
import org.wx.core.wxBusiness.common.service.BannerService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;
import org.wx.core.wxBusiness.log.annotation.WxThirdCallbackLog;
import org.wx.core.wxBusiness.media.entity.HzCrowdFundingContent;
import org.wx.core.wxBusiness.media.service.HzCrowdFundingContentService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * 前端 - 内容中心 (公告等)
 */
@RestController
@RequestMapping("/api/media")
public class A5MediaController {

    @Resource
    public ArticleService articleService;
    @Resource
    public BannerService bannerService;

    /**
     * 公告列表
     */
    @PostMapping("/article/list")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<Article>> articleList(
            Article entity
    ){
        IPage<Article> page = articleService.find().entity(entity).page();
        return WxResult.page(page);
    }

    /**
     * 轮播图列表
     */
    @PostMapping("/banner/list")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<Banner>> bannerList(
            Banner entity
    ){
        IPage<Banner> page = bannerService.find().entity(entity).page();
        return WxResult.page(page);
    }


}
