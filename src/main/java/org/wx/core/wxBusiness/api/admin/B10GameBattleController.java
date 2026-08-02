package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.game.entity.GameMonster;
import org.wx.core.wxBusiness.game.entity.GameWave;
import org.wx.core.wxBusiness.game.entity.GameWaveMonster;
import org.wx.core.wxBusiness.game.service.GameBattleService;
import org.wx.core.wxBusiness.game.service.GameMonsterService;
import org.wx.core.wxBusiness.game.service.GameWaveMonsterService;
import org.wx.core.wxBusiness.game.service.GameWaveService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-波次与怪物
 */
@RestController
@RequestMapping("/back/game/battle")
public class B10GameBattleController {

    @Resource
    private GameBattleService gameBattleService;
    @Resource
    private GameMonsterService monsterService;
    @Resource
    private GameWaveService waveService;
    @Resource
    private GameWaveMonsterService waveMonsterService;

    @PostMapping("/monster/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameMonster>> monsterList(@RequestBody GameMonster entity) {
        entity.clearEmptyString();
        IPage<GameMonster> page = monsterService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/monster/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> monsterSave(@RequestBody GameMonster entity) {
        entity.clearEmptyString();
        gameBattleService.saveMonster(entity);
        return WxResult.success();
    }

    @PostMapping("/wave/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameWave>> waveList(@RequestBody GameWave entity) {
        entity.clearEmptyString();
        IPage<GameWave> page = waveService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/wave/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> waveSave(@RequestBody GameWave entity) {
        entity.clearEmptyString();
        gameBattleService.saveWave(entity);
        return WxResult.success();
    }

    @PostMapping("/wave-monster/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameWaveMonster>> waveMonsterList(@RequestBody GameWaveMonster entity) {
        entity.clearEmptyString();
        if (entity.getWaveId() != null && !entity.getWaveId().isBlank()) {
            return WxResult.success(gameBattleService.listWaveMonsters(entity.getWaveId()));
        }
        IPage<GameWaveMonster> page = waveMonsterService.pageQuery(entity);
        return WxResult.page(page);
    }

    @PostMapping("/wave-monster/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> waveMonsterSave(@RequestBody GameWaveMonster entity) {
        gameBattleService.saveWaveMonster(entity);
        return WxResult.success();
    }

    @PostMapping("/stage/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<GameBattleService.StageBattleDetail> stageDetail(
            @ParamCheck(msg = "小关卡ID") String stageId
    ) {
        return WxResult.success(gameBattleService.getStageBattleDetail(stageId));
    }
}
