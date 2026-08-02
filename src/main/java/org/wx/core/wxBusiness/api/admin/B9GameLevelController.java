package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.service.GameChapterService;
import org.wx.core.wxBusiness.game.service.GameLevelService;
import org.wx.core.wxBusiness.game.service.GameModeGroupService;
import org.wx.core.wxBusiness.game.service.GameStageGroupService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-关卡管理
 */
@RestController
@RequestMapping("/back/game/level")
public class B9GameLevelController {

    @Resource
    private GameLevelService gameLevelService;
    @Resource
    private GameModeGroupService modeGroupService;
    @Resource
    private GameChapterService chapterService;
    @Resource
    private GameStageGroupService stageGroupService;

    @PostMapping("/tree")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<GameLevelService.LevelTree> tree() {
        return WxResult.success(gameLevelService.buildLevelTree());
    }

    @PostMapping("/mode/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameModeGroup>> modeList(@RequestBody GameModeGroup entity) {
        entity.clearEmptyString();
        IPage<GameModeGroup> page = modeGroupService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/mode/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> modeSave(@RequestBody GameModeGroup entity) {
        entity.clearEmptyString();
        gameLevelService.saveModeGroup(entity);
        return WxResult.success();
    }

    @PostMapping("/chapter/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameChapter>> chapterList(@RequestBody GameChapter entity) {
        entity.clearEmptyString();
        IPage<GameChapter> page = chapterService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/chapter/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> chapterSave(@RequestBody GameChapter entity) {
        entity.clearEmptyString();
        gameLevelService.saveChapter(entity);
        return WxResult.success();
    }

    @PostMapping("/stage-group/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameStageGroup>> stageGroupList(@RequestBody GameStageGroup entity) {
        entity.clearEmptyString();
        IPage<GameStageGroup> page = stageGroupService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/stage-group/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> stageGroupSave(@RequestBody GameStageGroup entity) {
        entity.clearEmptyString();
        gameLevelService.saveStageGroup(entity);
        return WxResult.success();
    }

    @PostMapping("/stage/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameStage>> stageList(@RequestBody GameStage entity) {
        entity.clearEmptyString();
        IPage<GameStage> page = gameLevelService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/stage/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> stageSave(@RequestBody GameStage entity) {
        entity.clearEmptyString();
        gameLevelService.saveStage(entity);
        return WxResult.success();
    }
}
