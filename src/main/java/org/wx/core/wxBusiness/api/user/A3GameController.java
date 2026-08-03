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
import org.wx.core.wxBusiness.game.entity.BattleMonsterDetailVo;
import org.wx.core.wxBusiness.game.entity.BattleState;
import org.wx.core.wxBusiness.game.entity.CraftRecipeVo;
import org.wx.core.wxBusiness.game.entity.ItemDetailVo;
import org.wx.core.wxBusiness.game.entity.ItemDropSourceVo;
import org.wx.core.wxBusiness.game.entity.MaterialSourceVo;
import org.wx.core.wxBusiness.game.service.GameBattleService;
import org.wx.core.wxBusiness.game.service.GameCraftService;
import org.wx.core.wxBusiness.game.service.GameItemDropSourceService;
import org.wx.core.wxBusiness.game.service.GameItemService;
import org.wx.core.wxBusiness.game.service.GameLevelService;
import org.wx.core.wxBusiness.game.service.GameMaterialSourceService;
import org.wx.core.wxBusiness.game.service.GamePrepService;
import org.wx.core.wxBusiness.game.service.PveBattleService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 前端-PVE游戏
 */
@RestController
@RequestMapping("/api/game")
public class A3GameController {

    @Resource
    private GameLevelService gameLevelService;
    @Resource
    private GameBattleService gameBattleService;
    @Resource
    private PveBattleService pveBattleService;
    @Resource
    private GameMaterialSourceService materialSourceService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameItemDropSourceService itemDropSourceService;
    @Resource
    private GameCraftService craftService;
    @Resource
    private GamePrepService gamePrepService;

    /**
     * 主角详情
     */
    @PostMapping("/hero/info")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<GameHero> heroInfo() {
        return WxResult.success(gamePrepService.getOutsideBattleHero(Wx.memberId()));
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
     * 跳过战斗至结束
     */
    @PostMapping("/battle/skip")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<BattleState> battleSkip(
            @ParamCheck(msg = "战斗ID") String battleId
    ) {
        return WxResult.success(pveBattleService.skipBattle(Wx.memberId(), battleId));
    }

    /**
     * 怪物详情与掉落
     */
    @PostMapping("/battle/monster/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<BattleMonsterDetailVo> battleMonsterDetail(
            @ParamCheck(msg = "怪物ID") String monsterId,
            @ParamCheck(msg = "关卡ID", notNull = false) String stageId
    ) {
        return WxResult.success(pveBattleService.getMonsterDetail(monsterId, stageId));
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

    /**
     * 材料来源：前往制作 / 前往出击 / 暂无来源
     */
    @PostMapping("/item/material/source")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<MaterialSourceVo> materialSource(
            @ParamCheck(msg = "物品ID") String itemId
    ) {
        return WxResult.success(materialSourceService.resolveSource(itemId));
    }

    /**
     * 物品详情（含合成公式）
     */
    @PostMapping("/item/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<ItemDetailVo> itemDetail(
            @ParamCheck(msg = "物品ID") String itemId
    ) {
        return WxResult.success(gameItemService.getItemDetail(itemId));
    }

    /**
     * 物品掉落关卡来源
     */
    @PostMapping("/item/drop-sources")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<ItemDropSourceVo>> itemDropSources(
            @ParamCheck(msg = "物品ID") String itemId
    ) {
        return WxResult.success(itemDropSourceService.listByItemId(itemId));
    }

    /**
     * 合成配方列表
     */
    @PostMapping("/craft/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<List<CraftRecipeVo>> craftList() {
        return WxResult.success(craftService.listRecipes(Wx.memberId()));
    }

    /**
     * 合成配方详情
     */
    @PostMapping("/craft/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<CraftRecipeVo> craftDetail(@ParamCheck(msg = "配方ID") String recipeId) {
        return WxResult.success(craftService.getRecipe(Wx.memberId(), recipeId));
    }

    /**
     * 执行合成
     */
    @PostMapping("/craft/execute")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.USER)
    public WxResult<CraftRecipeVo> craftExecute(@ParamCheck(msg = "配方ID") String recipeId) {
        return WxResult.success(craftService.craft(Wx.memberId(), recipeId));
    }
}
