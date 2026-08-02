package org.wx.core.wxBusiness.api.user;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.game.entity.GameHero;
import org.wx.core.wxBusiness.game.service.GameHeroService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

/**
 * 前端-PVE游戏
 */
@RestController
@RequestMapping("/api/game")
public class A3GameController {

    @Resource
    private GameHeroService gameHeroService;

    /**
     * 主角详情
     */
    @PostMapping("/hero/info")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<GameHero> heroInfo() {
        return WxResult.success(gameHeroService.getOrInitHero(Wx.memberId()));
    }
}
