package org.wx.core.wxBusiness.game.service;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.engine.BattleEngine;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.MonsterRank;
import org.wx.core.wxBusiness.game.service.GameBattleService.StageBattleDetail;
import org.wx.core.wxBusiness.game.service.GameBattleService.WaveDetailNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PveBattleService {

    private static final String REDIS_PREFIX = "pve-battle:";
    private static final String REDIS_INITIAL_PREFIX = "pve-battle-initial:";
    private static final String REDIS_FINAL_PREFIX = "pve-battle-final:";
    private static final long REDIS_HOURS = 2;

    /** 开战后在后台匀速跑完整场，供跳过直接取终态 */
    private final ConcurrentHashMap<String, CompletableFuture<BattleState>> pendingFinals = new ConcurrentHashMap<>();

    @Resource
    private GameHeroService gameHeroService;
    @Resource
    private GameBattleService gameBattleService;
    @Resource
    private GameTriggerSlotEngineService triggerSlotEngineService;
    @Resource
    private GameWeaponService gameWeaponService;
    @Resource
    private GameMonsterService gameMonsterService;
    @Resource
    private GameMonsterDropService gameMonsterDropService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameStageDropService gameStageDropService;
    @Resource
    private MonsterDropService monsterDropService;
    @Resource
    private GameInventoryService gameInventoryService;
    @Resource
    private GameHeroEquipService gameHeroEquipService;
    @Resource
    private GamePrepService gamePrepService;
    @Resource
    private MonsterCombatService monsterCombatService;
    @Resource
    private BattleFormationService battleFormationService;

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
        state.setTriggerCounters(new BattleTriggerCounters());
        state.setLootAccumulated(new ArrayList<>());
        state.setLootGranted(false);

        spawnHero(state, hero);
        spawnWaveMonsters(state, waves.get(0), 1);
        state.appendLog(BattleLog.of(BattleLog.TYPE_WAVE, "第 1 波开始"));
        saveState(uid, state);
        saveInitialSnapshot(uid, state.getBattleId(), state);
        scheduleFinalSimulation(uid, state.getBattleId(), state);
        return state;
    }

    public BattleState nextStep(String uid, String battleId) {
        BattleState state = requireBattle(uid, battleId);
        if (!state.isRunning()) {
            return state;
        }
        advanceOneTurn(uid, state);
        saveState(uid, state);
        return state;
    }

    public BattleState getBattle(String uid, String battleId) {
        return requireBattle(uid, battleId);
    }

    public BattleState skipBattle(String uid, String battleId) {
        requireBattle(uid, battleId);
        BattleState finalState = awaitFinalState(uid, battleId);
        if (BattleState.STATUS_WIN.equals(finalState.getStatus())) {
            applyLootGrant(uid, finalState);
        }
        saveState(uid, finalState);
        cleanupFinalResources(uid, battleId);
        return finalState;
    }

    public boolean isResultReady(String uid, String battleId) {
        if (loadFinalState(uid, battleId) != null) {
            return true;
        }
        CompletableFuture<BattleState> future = pendingFinals.get(finalTaskKey(uid, battleId));
        return future != null && future.isDone() && !future.isCompletedExceptionally();
    }

    private void advanceOneTurn(String uid, BattleState state) {
        advanceOneTurn(uid, state, true);
    }

    private void advanceOneTurn(String uid, BattleState state, boolean grantLoot) {
        if (!state.isRunning()) {
            return;
        }
        List<BattleLog> tickLogs = new ArrayList<>();
        BattleEngine.advanceUntilReady(state, (unit, gain) -> {
            List<BattleLog> ticked = triggerSlotEngineService.onActionValueTick(state, unit, gain);
            if (!ticked.isEmpty()) {
                tickLogs.addAll(ticked);
            }
        });
        state.appendLogs(tickLogs);

        BattleUnit actor = BattleEngine.pickReadyUnit(state);
        if (actor == null) {
            resolveEndState(state);
            if (grantLoot && BattleState.STATUS_WIN.equals(state.getStatus())) {
                applyLootGrant(uid, state);
            }
            return;
        }
        List<BattleLog> turnLogs = triggerSlotEngineService.onActionValueFull(state, actor);
        if (turnLogs.isEmpty()) {
            BattleLog fallback = BattleEngine.performAction(state, actor);
            if (fallback != null) {
                state.appendLog(fallback);
                turnLogs = List.of(fallback);
            }
        } else {
            state.appendLogs(turnLogs);
        }
        if (actor.isAlive()) {
            BattleEngine.resetActionBar(actor);
        }
        handleKillDrops(state, actor, turnLogs);
        resolveAfterAction(uid, state, grantLoot);
    }

    private void handleKillDrops(BattleState state, BattleUnit actor, List<BattleLog> turnLogs) {
        if (!BattleUnit.SIDE_HERO.equals(actor.getSide()) || turnLogs == null) {
            return;
        }
        for (BattleLog actionLog : turnLogs) {
            if (!Boolean.TRUE.equals(actionLog.getKilled())) {
                continue;
            }
            if (!BattleLog.TYPE_ACTION.equals(actionLog.getType())
                    && !BattleLog.TYPE_SKILL.equals(actionLog.getType())) {
                continue;
            }
            BattleUnit deadMonster = state.getUnits().stream()
                    .filter(u -> BattleUnit.SIDE_MONSTER.equals(u.getSide()))
                    .filter(u -> actionLog.getTargetName() != null && actionLog.getTargetName().equals(u.getName()))
                    .findFirst()
                    .orElse(null);
            if (deadMonster == null || deadMonster.getMonsterId() == null) {
                continue;
            }
            List<BattleLootEntry> drops = monsterDropService.rollDrops(deadMonster.getMonsterId());
            if (drops.isEmpty()) {
                continue;
            }
            mergeLoot(state, drops);
            for (BattleLootEntry drop : drops) {
                state.appendLog(BattleLog.loot("获得 " + drop.getItemName() + " x" + drop.getQuantity()));
            }
        }
    }

    private void mergeLoot(BattleState state, List<BattleLootEntry> drops) {
        if (state.getLootAccumulated() == null) {
            state.setLootAccumulated(new ArrayList<>());
        }
        for (BattleLootEntry drop : drops) {
            BattleLootEntry existing = state.getLootAccumulated().stream()
                    .filter(e -> drop.getItemId().equals(e.getItemId()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + drop.getQuantity());
            } else {
                BattleLootEntry copy = new BattleLootEntry();
                copy.setItemId(drop.getItemId());
                copy.setItemCode(drop.getItemCode());
                copy.setItemName(drop.getItemName());
                copy.setIcon(drop.getIcon());
                copy.setQuantity(drop.getQuantity());
                state.getLootAccumulated().add(copy);
            }
        }
    }

    private void applyLootGrant(String uid, BattleState state) {
        if (Boolean.TRUE.equals(state.getLootGranted())
                || state.getLootAccumulated() == null
                || state.getLootAccumulated().isEmpty()) {
            return;
        }
        gameInventoryService.grantBattleLoot(uid, state.getLootAccumulated(), state.getBattleId());
        state.setLootGranted(true);
        state.appendLog(BattleLog.of(BattleLog.TYPE_RESULT, "战利品已放入仓库"));
    }

    private void finishWin(String uid, BattleState state, boolean grantLoot) {
        state.setStatus(BattleState.STATUS_WIN);
        if (grantLoot) {
            applyLootGrant(uid, state);
        }
        state.appendLog(BattleLog.of(BattleLog.TYPE_RESULT, "怪物全灭，战斗胜利"));
    }

    private void simulateToEnd(String uid, BattleState state) {
        int guard = 0;
        while (state.isRunning() && guard++ < 10000) {
            advanceOneTurn(uid, state, false);
        }
    }

    private void scheduleFinalSimulation(String uid, String battleId, BattleState initial) {
        String taskKey = finalTaskKey(uid, battleId);
        CompletableFuture<BattleState> future = CompletableFuture.supplyAsync(() -> {
            BattleState sim = cloneState(initial);
            simulateToEnd(uid, sim);
            saveFinalState(uid, battleId, sim);
            return sim;
        });
        pendingFinals.put(taskKey, future);
        future.whenComplete((result, error) -> pendingFinals.remove(taskKey));
    }

    private BattleState awaitFinalState(String uid, String battleId) {
        BattleState cached = loadFinalState(uid, battleId);
        if (cached != null) {
            return cached;
        }
        CompletableFuture<BattleState> future = pendingFinals.get(finalTaskKey(uid, battleId));
        if (future != null) {
            try {
                return future.get(60, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // fall through to sync simulate
            }
        }
        BattleState initial = loadInitialSnapshot(uid, battleId);
        ErrorFactory.notNull(initial, "战斗初始状态不存在");
        BattleState sim = cloneState(initial);
        simulateToEnd(uid, sim);
        saveFinalState(uid, battleId, sim);
        return sim;
    }

    private BattleState cloneState(BattleState state) {
        return JSON.parseObject(JSON.toJSONString(state), BattleState.class);
    }

    public BattleMonsterDetailVo getMonsterDetail(String monsterId, String stageId) {
        ErrorFactory.notNull(monsterId, "怪物ID不能为空");
        GameMonster monster = gameMonsterService.getById(monsterId);
        ErrorFactory.notNull(monster, "怪物不存在");

        BattleMonsterDetailVo vo = new BattleMonsterDetailVo();
        vo.setMonsterId(monster.getId());
        vo.setCode(monster.getCode());
        vo.setName(monster.getName());
        vo.setHp(monster.getHp());
        vo.setMaxHp(monster.getMaxHp());
        vo.setAttack(monster.getAttack());
        vo.setActionValue(monster.getActionValue());
        MonsterRank rank = MonsterRank.parse(monster.getRankType());
        vo.setRankType(rank.name());
        vo.setRankLabel(rank.getLabel());
        vo.setFootprintW(monster.getFootprintW() != null ? monster.getFootprintW() : rank.getFootprintW());
        vo.setFootprintH(monster.getFootprintH() != null ? monster.getFootprintH() : rank.getFootprintH());
        vo.setRemark(monster.getRemark());

        java.util.LinkedHashMap<String, BattleMonsterDropItemVo> dropMap = new java.util.LinkedHashMap<>();
        for (GameMonsterDrop drop : gameMonsterDropService.listEnabledByMonsterId(monsterId)) {
            addMonsterDropItem(dropMap, drop);
        }
        if (stageId != null && !stageId.isBlank()) {
            for (GameStageDrop drop : gameStageDropService.find()
                    .eq(GameStageDrop::getStageId, stageId)
                    .eq(GameStageDrop::getEnabled, 1)
                    .list()) {
                addStageDropItem(dropMap, drop.getItemId());
            }
        }
        vo.setDrops(new ArrayList<>(dropMap.values()));
        return vo;
    }

    private void addMonsterDropItem(java.util.LinkedHashMap<String, BattleMonsterDropItemVo> dropMap,
                                    GameMonsterDrop drop) {
        if (drop == null || drop.getItemId() == null || drop.getItemId().isBlank()) {
            return;
        }
        GameItem item = gameItemService.getById(drop.getItemId());
        if (item == null) {
            return;
        }
        BattleMonsterDropItemVo dropVo = dropMap.get(drop.getItemId());
        if (dropVo == null) {
            dropVo = new BattleMonsterDropItemVo();
            dropVo.setItemId(item.getId());
            dropVo.setItemName(item.getName());
            dropVo.setIcon(item.getIcon());
            dropMap.put(drop.getItemId(), dropVo);
        }
        dropVo.setDropRate(drop.getDropRate());
        dropVo.setMinQty(drop.getMinQty());
        dropVo.setMaxQty(drop.getMaxQty());
    }

    private void addStageDropItem(java.util.LinkedHashMap<String, BattleMonsterDropItemVo> dropMap, String itemId) {
        if (itemId == null || itemId.isBlank() || dropMap.containsKey(itemId)) {
            return;
        }
        GameItem item = gameItemService.getById(itemId);
        if (item == null) {
            return;
        }
        BattleMonsterDropItemVo dropVo = new BattleMonsterDropItemVo();
        dropVo.setItemId(item.getId());
        dropVo.setItemName(item.getName());
        dropVo.setIcon(item.getIcon());
        dropMap.put(itemId, dropVo);
    }

    private void resolveAfterAction(String uid, BattleState state, boolean grantLoot) {
        BattleEngine.refreshAliveState(state);

        if (BattleEngine.heroDead(state)) {
            state.setStatus(BattleState.STATUS_LOSE);
            state.appendLog(BattleLog.of(BattleLog.TYPE_RESULT, "主角阵亡，战斗失败"));
            return;
        }

        if (!BattleEngine.allMonstersDead(state)) {
            return;
        }

        BattleEngine.removeDeadMonsters(state);
        int nextWave = state.getCurrentWave() + 1;
        if (nextWave > state.getTotalWaves()) {
            finishWin(uid, state, grantLoot);
            return;
        }

        StageBattleDetail detail = gameBattleService.getStageBattleDetail(state.getStageId());
        List<WaveDetailNode> waves = detail.getWaves().stream()
                .filter(w -> w.getWave() != null && Integer.valueOf(1).equals(w.getWave().getEnabled()))
                .collect(Collectors.toList());
        if (nextWave > waves.size()) {
            finishWin(uid, state, grantLoot);
            return;
        }

        state.setCurrentWave(nextWave);
        spawnWaveMonsters(state, waves.get(nextWave - 1), nextWave);
        state.appendLog(BattleLog.of(BattleLog.TYPE_WAVE, "第 " + nextWave + " 波开始"));
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
        int totalMaxHp = gamePrepService.resolveBattleMaxHp(state.getUid());
        unit.setMaxHp(totalMaxHp);
        int startHp = hero.getHp() != null ? hero.getHp() : totalMaxHp;
        unit.setHp(Math.min(Math.max(startHp, 0), totalMaxHp));
        unit.setAttack(gamePrepService.resolveBattleTotalAttack(state.getUid()));
        unit.setDefense(gamePrepService.resolveBattleDefense(state.getUid()));
        unit.setActionValue(gamePrepService.resolveBattleActionValue(state.getUid()));
        unit.setAlive(true);
        unit.initActionBar();
        state.setHeroEquippedItemIds(gameHeroEquipService.getOrInit(state.getUid()).listEquippedItemIds());
        applyHeroWeaponRatio(state, unit);
        battleFormationService.placeHero(unit);
        state.getUnits().add(unit);
    }

    private void applyHeroWeaponRatio(BattleState state, BattleUnit unit) {
        List<String> itemIds = state.getHeroEquippedItemIds();
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        for (String itemId : itemIds) {
            GameWeapon weapon = gameWeaponService.getByItemId(itemId);
            if (weapon != null && weapon.getDamageRatio() != null) {
                unit.setWeaponDamageRatio(weapon.getDamageRatio());
                return;
            }
        }
    }

    private void spawnWaveMonsters(BattleState state, WaveDetailNode waveNode, int waveNo) {
        int idx = 0;
        List<BattleUnit> spawned = new ArrayList<>();
        for (GameWaveMonster wm : waveNode.getMonsters()) {
            int quantity = wm.getQuantity() != null ? wm.getQuantity() : 1;
            String baseName = wm.getMonsterName() != null ? wm.getMonsterName() : "怪物";
            GameMonster template = gameMonsterService.getById(wm.getMonsterId());
            for (int q = 0; q < quantity; q++) {
                BattleUnit unit = new BattleUnit();
                unit.setUnitId("mon_" + waveNo + "_" + idx++);
                unit.setSide(BattleUnit.SIDE_MONSTER);
                unit.setName(quantity > 1 ? baseName + "#" + (q + 1) : baseName);
                unit.setMonsterId(wm.getMonsterId());
                unit.setHp(wm.getHp());
                unit.setMaxHp(wm.getMaxHp());
                unit.setAttack(wm.getAttack());
                unit.setDefense(0);
                unit.setActionValue(wm.getActionValue());
                unit.setAlive(true);
                unit.initActionBar();
                battleFormationService.applyMonsterTemplate(unit, template);
                // quantity>1 时仅第一个实例使用配置站位，其余自动找空位
                if (q == 0 && wm.getSlotCol() != null && wm.getSlotRow() != null) {
                    unit.setSlotCol(wm.getSlotCol());
                    unit.setSlotRow(wm.getSlotRow());
                }
                monsterCombatService.applyPassives(unit, wm.getMonsterId());
                spawned.add(unit);
            }
        }
        battleFormationService.placeMonsters(spawned);
        state.getUnits().addAll(spawned);
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
        BattleState state = JSON.parseObject(json, BattleState.class);
        if (state != null && state.getTriggerCounters() == null) {
            state.setTriggerCounters(new BattleTriggerCounters());
        }
        return state;
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

    private String initialKey(String uid, String battleId) {
        return REDIS_INITIAL_PREFIX + uid + ":" + battleId;
    }

    private String finalKey(String uid, String battleId) {
        return REDIS_FINAL_PREFIX + uid + ":" + battleId;
    }

    private String finalTaskKey(String uid, String battleId) {
        return uid + ":" + battleId;
    }

    private void saveInitialSnapshot(String uid, String battleId, BattleState state) {
        Wx.RedisFactory.setStrBuyHour(initialKey(uid, battleId), JSON.toJSONString(state), REDIS_HOURS);
    }

    private BattleState loadInitialSnapshot(String uid, String battleId) {
        String json = Wx.RedisFactory.getStr(initialKey(uid, battleId));
        if (json == null || json.isBlank()) {
            return null;
        }
        json = unwrapJsonString(json);
        return JSON.parseObject(json, BattleState.class);
    }

    private void saveFinalState(String uid, String battleId, BattleState state) {
        Wx.RedisFactory.setStrBuyHour(finalKey(uid, battleId), JSON.toJSONString(state), REDIS_HOURS);
    }

    private BattleState loadFinalState(String uid, String battleId) {
        String json = Wx.RedisFactory.getStr(finalKey(uid, battleId));
        if (json == null || json.isBlank()) {
            return null;
        }
        json = unwrapJsonString(json);
        return JSON.parseObject(json, BattleState.class);
    }

    private void cleanupFinalResources(String uid, String battleId) {
        pendingFinals.remove(finalTaskKey(uid, battleId));
        Wx.RedisFactory.del(initialKey(uid, battleId));
        Wx.RedisFactory.del(finalKey(uid, battleId));
    }

    /** 后台清空用户战斗缓存（进行中的 PVE） */
    public void clearUserBattleCache(String uid) {
        if (uid == null || uid.isBlank()) {
            return;
        }
        BattleState state = loadState(uid);
        if (state != null && state.getBattleId() != null) {
            cleanupFinalResources(uid, state.getBattleId());
        }
        Wx.RedisFactory.del(redisKey(uid));
    }
}
