package org.wx.core.wxBusiness.game.service;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.engine.BattleEngine;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.service.GameBattleService.StageBattleDetail;
import org.wx.core.wxBusiness.game.service.GameBattleService.WaveDetailNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PveBattleService {

    private static final String REDIS_PREFIX = "pve-battle:";
    private static final long REDIS_HOURS = 2;

    @Resource
    private GameHeroService gameHeroService;
    @Resource
    private GameBattleService gameBattleService;

    public BattleState startBattle(String uid, String stageId) {
        GameHero hero = gameHeroService.getOrInitHero(uid);
        StageBattleDetail detail = gameBattleService.getStageBattleDetail(stageId);
        List<WaveDetailNode> waves = detail.getWaves().stream()
                .filter(w -> w.getWave() != null && Integer.valueOf(1).equals(w.getWave().getEnabled()))
                .collect(Collectors.toList());
        ErrorFactory.throwError(waves.isEmpty(), "该关卡没有可用波次");

        BattleState state = new BattleState();
        state.setBattleId(WordUnit.randomKey(12, 1));
        state.setStageId(stageId);
        state.setStageName(detail.getStage().getName());
        state.setDisplayCode(detail.getStage().getDisplayCode());
        state.setUid(uid);
        state.setStatus(BattleState.STATUS_RUNNING);
        state.setCurrentWave(1);
        state.setTotalWaves(waves.size());
        state.setActionTickGain(BattleEngine.DEFAULT_TICK_GAIN);
        state.setUnits(new ArrayList<>());
        state.setLogs(new ArrayList<>());

        spawnHero(state, hero);
        spawnWaveMonsters(state, waves.get(0), 1);
        state.getLogs().add(BattleLog.of(BattleLog.TYPE_WAVE, "第 1 波开始"));
        saveState(uid, state);
        return state;
    }

    public BattleState nextStep(String uid, String battleId) {
        BattleState state = requireBattle(uid, battleId);
        if (!state.isRunning()) {
            return state;
        }

        BattleEngine.advanceUntilReady(state);
        BattleUnit actor = BattleEngine.pickReadyUnit(state);
        if (actor == null) {
            resolveEndState(state);
            saveState(uid, state);
            return state;
        }

        BattleLog actionLog = BattleEngine.performAction(state, actor);
        state.getLogs().add(actionLog);
        resolveAfterAction(uid, state);
        saveState(uid, state);
        return state;
    }

    public BattleState getBattle(String uid, String battleId) {
        return requireBattle(uid, battleId);
    }

    private void resolveAfterAction(String uid, BattleState state) {
        BattleEngine.refreshAliveState(state);

        if (BattleEngine.heroDead(state)) {
            state.setStatus(BattleState.STATUS_LOSE);
            state.getLogs().add(BattleLog.of(BattleLog.TYPE_RESULT, "主角阵亡，战斗失败"));
            return;
        }

        if (!BattleEngine.allMonstersDead(state)) {
            return;
        }

        BattleEngine.removeDeadMonsters(state);
        int nextWave = state.getCurrentWave() + 1;
        if (nextWave > state.getTotalWaves()) {
            state.setStatus(BattleState.STATUS_WIN);
            state.getLogs().add(BattleLog.of(BattleLog.TYPE_RESULT, "怪物全灭，战斗胜利"));
            return;
        }

        StageBattleDetail detail = gameBattleService.getStageBattleDetail(state.getStageId());
        List<WaveDetailNode> waves = detail.getWaves().stream()
                .filter(w -> w.getWave() != null && Integer.valueOf(1).equals(w.getWave().getEnabled()))
                .collect(Collectors.toList());
        if (nextWave > waves.size()) {
            state.setStatus(BattleState.STATUS_WIN);
            state.getLogs().add(BattleLog.of(BattleLog.TYPE_RESULT, "怪物全灭，战斗胜利"));
            return;
        }

        state.setCurrentWave(nextWave);
        spawnWaveMonsters(state, waves.get(nextWave - 1), nextWave);
        state.getLogs().add(BattleLog.of(BattleLog.TYPE_WAVE, "第 " + nextWave + " 波开始"));
    }

    private void resolveEndState(BattleState state) {
        if (BattleEngine.heroDead(state)) {
            state.setStatus(BattleState.STATUS_LOSE);
        } else if (BattleEngine.allMonstersDead(state) && state.getCurrentWave() >= state.getTotalWaves()) {
            state.setStatus(BattleState.STATUS_WIN);
        }
    }

    private void spawnHero(BattleState state, GameHero hero) {
        BattleUnit unit = new BattleUnit();
        unit.setUnitId("hero");
        unit.setSide(BattleUnit.SIDE_HERO);
        unit.setName(hero.getName() != null ? hero.getName() : GameHero.DEFAULT_NAME);
        unit.setHp(hero.getHp());
        unit.setMaxHp(hero.getMaxHp());
        unit.setAttack(hero.getAttack());
        unit.setActionValue(hero.getActionValue());
        unit.setAlive(true);
        unit.initActionBar();
        state.getUnits().add(unit);
    }

    private void spawnWaveMonsters(BattleState state, WaveDetailNode waveNode, int waveNo) {
        int idx = 0;
        for (GameWaveMonster wm : waveNode.getMonsters()) {
            int quantity = wm.getQuantity() != null ? wm.getQuantity() : 1;
            String baseName = wm.getMonsterName() != null ? wm.getMonsterName() : "怪物";
            for (int q = 0; q < quantity; q++) {
                BattleUnit unit = new BattleUnit();
                unit.setUnitId("mon_" + waveNo + "_" + idx++);
                unit.setSide(BattleUnit.SIDE_MONSTER);
                unit.setName(quantity > 1 ? baseName + "#" + (q + 1) : baseName);
                unit.setMonsterId(wm.getMonsterId());
                unit.setHp(wm.getHp());
                unit.setMaxHp(wm.getMaxHp());
                unit.setAttack(wm.getAttack());
                unit.setActionValue(wm.getActionValue());
                unit.setAlive(true);
                unit.initActionBar();
                state.getUnits().add(unit);
            }
        }
    }

    private BattleState requireBattle(String uid, String battleId) {
        BattleState state = loadState(uid);
        ErrorFactory.notNull(state, "战斗不存在或已过期");
        ErrorFactory.notEquals(battleId, state.getBattleId(), "战斗ID不匹配");
        return state;
    }

    private void saveState(String uid, BattleState state) {
        Wx.RedisFactory.setStrBuyHour(redisKey(uid), JSON.toJSONString(state), REDIS_HOURS);
    }

    private BattleState loadState(String uid) {
        String json = Wx.RedisFactory.getStr(redisKey(uid));
        if (json == null || json.isBlank()) {
            return null;
        }
        json = unwrapJsonString(json);
        return JSON.parseObject(json, BattleState.class);
    }

    /** 兼容 RedisTemplate 二次序列化后的旧数据 */
    private String unwrapJsonString(String json) {
        String trimmed = json.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return JSON.parseObject(trimmed, String.class);
        }
        return json;
    }

    private String redisKey(String uid) {
        return REDIS_PREFIX + uid;
    }
}
