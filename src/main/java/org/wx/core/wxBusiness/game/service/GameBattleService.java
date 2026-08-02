package org.wx.core.wxBusiness.game.service;

import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameBattleService {

    @Resource
    private GameMonsterService monsterService;
    @Resource
    private GameWaveService waveService;
    @Resource
    private GameWaveMonsterService waveMonsterService;
    @Resource
    private GameLevelService gameLevelService;
    @Resource
    private GameStageGroupService stageGroupService;

    @Transactional(rollbackFor = Exception.class)
    public void saveMonster(GameMonster entity) {
        ErrorFactory.notNull(entity.getCode(), "怪物编码不能为空");
        ErrorFactory.notNull(entity.getName(), "怪物名称不能为空");
        if (entity.getHp() == null) entity.setHp(100);
        if (entity.getMaxHp() == null) entity.setMaxHp(entity.getHp());
        if (entity.getAttack() == null) entity.setAttack(10);
        if (entity.getActionValue() == null) entity.setActionValue(100);
        if (entity.getEnabled() == null) entity.setEnabled(1);
        if (entity.getSort() == null) entity.setSort(0);
        monsterService.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveWave(GameWave entity) {
        ErrorFactory.notNull(entity.getStageId(), "小关卡不能为空");
        ErrorFactory.notNull(entity.getWaveNo(), "波次序号不能为空");
        if (entity.getEnabled() == null) entity.setEnabled(1);
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getName() == null || entity.getName().isBlank()) {
            entity.setName("第" + entity.getWaveNo() + "波");
        }
        waveService.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveWaveMonster(GameWaveMonster entity) {
        ErrorFactory.notNull(entity.getWaveId(), "波次不能为空");
        ErrorFactory.notNull(entity.getMonsterId(), "怪物不能为空");
        if (entity.getQuantity() == null || entity.getQuantity() < 1) {
            entity.setQuantity(1);
        }
        if (entity.getSort() == null) entity.setSort(0);
        waveMonsterService.saveOrUpdate(entity);
    }

    public int countWavesByStageId(String stageId) {
        return (int) waveService.find()
                .eq(GameWave::getStageId, stageId)
                .eq(GameWave::getEnabled, 1)
                .count();
    }

    public List<GameStageSelectVo> listSelectableStages(String chapterId) {
        List<GameStage> stages = gameLevelService.listStagesByChapterId(chapterId);
        List<GameStageSelectVo> result = new ArrayList<>();
        for (GameStage stage : stages) {
            GameStageSelectVo vo = new GameStageSelectVo();
            vo.setId(stage.getId());
            vo.setName(stage.getName());
            vo.setDisplayCode(stage.getDisplayCode());
            vo.setGroupNo(stage.getGroupNo());
            vo.setStageNo(stage.getStageNo());
            vo.setWaveCount(countWavesByStageId(stage.getId()));
            result.add(vo);
        }
        return result;
    }

    public StageBattleDetail getStageBattleDetail(String stageId) {
        GameStage stage = gameLevelService.getById(stageId);
        ErrorFactory.notNull(stage, "小关卡不存在");

        GameStageGroup stageGroup = stageGroupService.getById(stage.getStageGroupId());
        if (stageGroup != null) {
            stage.fillDisplayCode(stageGroup.getGroupNo());
        }

        StageBattleDetail detail = new StageBattleDetail();
        detail.setStage(stage);
        List<GameWave> waves = waveService.find()
                .eq(GameWave::getStageId, stageId)
                .orderByAsc(GameWave::getSort)
                .orderByAsc(GameWave::getWaveNo)
                .list();
        for (GameWave wave : waves) {
            WaveDetailNode waveNode = new WaveDetailNode();
            waveNode.setWave(wave);
            waveNode.setMonsters(listWaveMonsters(wave.getId()));
            detail.getWaves().add(waveNode);
        }
        return detail;
    }

    public List<GameWaveMonster> listWaveMonsters(String waveId) {
        List<GameWaveMonster> rows = waveMonsterService.find()
                .eq(GameWaveMonster::getWaveId, waveId)
                .orderByAsc(GameWaveMonster::getSort)
                .list();
        if (rows.isEmpty()) {
            return rows;
        }
        List<String> monsterIds = rows.stream().map(GameWaveMonster::getMonsterId).distinct().toList();
        Map<String, GameMonster> monsterMap = monsterService.listByIds(monsterIds).stream()
                .collect(Collectors.toMap(GameMonster::getId, m -> m));
        for (GameWaveMonster row : rows) {
            GameMonster monster = monsterMap.get(row.getMonsterId());
            if (monster != null) {
                row.setMonsterName(monster.getName());
                row.setMonsterCode(monster.getCode());
                row.setHp(monster.getHp());
                row.setMaxHp(monster.getMaxHp());
                row.setAttack(monster.getAttack());
                row.setActionValue(monster.getActionValue());
            }
        }
        return rows;
    }

    @Data
    public static class StageBattleDetail {
        private GameStage stage;
        private List<WaveDetailNode> waves = new ArrayList<>();
    }

    @Data
    public static class WaveDetailNode {
        private GameWave wave;
        private List<GameWaveMonster> monsters = new ArrayList<>();
    }
}
