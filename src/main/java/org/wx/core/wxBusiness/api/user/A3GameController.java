package org.wx.core.wxBusiness.api.user;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.game.entity.GameHero;
import org.wx.core.wxBusiness.game.entity.GameStage;
import org.wx.core.wxBusiness.game.entity.GameStageSelectVo;
import org.wx.core.wxBusiness.game.entity.BattleState;
import org.wx.core.wxBusiness.game.service.GameBattleService;
import org.wx.core.wxBusiness.game.service.GameHeroService;
import org.wx.core.wxBusiness.game.service.GameLevelService;
import org.wx.core.wxBusiness.game.service.PveBattleService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

/**
 * 前端-PVE游戏
 */
@RestController
@RequestMapping("/api/game")
public class A3GameController {

    @Resource
    private GameHeroService gameHeroService;
    @Resource
    private GameLevelService gameLevelService;
    @Resource
    private GameBattleService gameBattleService;
    @Resource
    private PveBattleService pveBattleService;

    /**
     * 主角详情
     */
    @PostMapping("/hero/info")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<GameHero> heroInfo() {
        return WxResult.success(gameHeroService.getOrInitHero(Wx.memberId()));
    }

    /**
     * 关卡树（玩家端）
     */
    @PostMapping("/level/tree")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<GameLevelService.LevelTree> levelTree() {
        return WxResult.success(gameLevelService.buildLevelTree());
    }

    /**
     * 小关卡列表（按大关卡）
     */
    @PostMapping("/level/stage/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<java.util.List<GameStage>> stageList(
            @ParamCheck(msg = "大关卡ID") String chapterId
    ) {
        return WxResult.success(gameLevelService.listStagesByChapterId(chapterId));
    }

    /**
     * 选关列表（仅小关，不含波次）
     */
    @PostMapping("/level/stage/select/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<java.util.List<GameStageSelectVo>> stageSelectList(
            @ParamCheck(msg = "大关卡ID", notNull = false) String chapterId
    ) {
        if (chapterId == null || chapterId.isBlank()) {
            chapterId = "chapter_main";
        }
        return WxResult.success(gameBattleService.listSelectableStages(chapterId));
    }

    /**
     * 开始战斗
     */
    @PostMapping("/battle/start")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<BattleState> battleStart(
            @ParamCheck(msg = "小关卡ID") String stageId
    ) {
        return WxResult.success(pveBattleService.startBattle(Wx.memberId(), stageId));
    }

    /**
     * 战斗下一步（行动条推进至可出手并执行一次攻击）
     */
    @PostMapping("/battle/next")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<BattleState> battleNext(
            @ParamCheck(msg = "战斗ID") String battleId
    ) {
        return WxResult.success(pveBattleService.nextStep(Wx.memberId(), battleId));
    }

    /**
     * 战斗状态
     */
    @PostMapping("/battle/state")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<BattleState> battleState(
            @ParamCheck(msg = "战斗ID") String battleId
    ) {
        return WxResult.success(pveBattleService.getBattle(Wx.memberId(), battleId));
    }
}
