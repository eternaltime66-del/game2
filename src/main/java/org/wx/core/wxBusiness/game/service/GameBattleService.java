package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.MonsterRank;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
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
    @Resource
    private GameReferenceCleanupService referenceCleanupService;

    @Transactional(rollbackFor = Exception.class)
    public void saveMonster(GameMonster entity) {
        ErrorFactory.notNull(entity.getCode(), "怪物编码不能为空");
        ErrorFactory.notNull(entity.getName(), "怪物名称不能为空");
        if (entity.getHp() == null) entity.setHp(100);
        if (entity.getMaxHp() == null) entity.setMaxHp(entity.getHp());
        if (entity.getAttack() == null) entity.setAttack(10);
        if (entity.getActionValue() == null) entity.setActionValue(100);
        MonsterRank rank = MonsterRank.parse(entity.getRankType());
        entity.setRankType(rank.name());
        if (entity.getFootprintW() == null || entity.getFootprintW() < 1) {
            entity.setFootprintW(rank.getFootprintW());
        }
        if (entity.getFootprintH() == null || entity.getFootprintH() < 1) {
            entity.setFootprintH(rank.getFootprintH());
        }
        if (entity.getEnabled() == null) entity.setEnabled(1);
        if (entity.getSort() == null) entity.setSort(0);
        monsterService.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMonster(String id) {
        ErrorFactory.notNull(id, "怪物ID不能为空");
        waveMonsterService.remove(new LambdaQueryWrapper<GameWaveMonster>()
                .eq(GameWaveMonster::getMonsterId, id));
        referenceCleanupService.removeMonsterBindings(id);
        monsterService.removeById(id);
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
        if (entity.getSlotCol() != null) {
            entity.setSlotCol(Math.max(0, Math.min(3, entity.getSlotCol())));
        }
        if (entity.getSlotRow() != null) {
            entity.setSlotRow(Math.max(0, Math.min(2, entity.getSlotRow())));
        }
        // 新增多只时拆成 quantity=1，便于精确站位
        boolean isNew = entity.getId() == null || entity.getId().isBlank();
        if (isNew && entity.getQuantity() > 1) {
            int qty = entity.getQuantity();
            int baseSort = entity.getSort() != null ? entity.getSort() : 0;
            for (int i = 0; i < qty; i++) {
                GameWaveMonster row = new GameWaveMonster();
                row.setWaveId(entity.getWaveId());
                row.setMonsterId(entity.getMonsterId());
                row.setQuantity(1);
                row.setSort(baseSort + i);
                if (i == 0 && entity.getSlotCol() != null && entity.getSlotRow() != null) {
                    row.setSlotCol(entity.getSlotCol());
                    row.setSlotRow(entity.getSlotRow());
                }
                waveMonsterService.save(row);
            }
            return;
        }
        waveMonsterService.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeWaveMonster(String id) {
        ErrorFactory.notNull(id, "波次怪物ID不能为空");
        waveMonsterService.removeById(id);
    }

    /**
     * 一键布置：拆分数量>1，按占地从小到大、前排优先、对称居中落位。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<GameWaveMonster> autoPlaceWaveMonsters(String waveId) {
        ErrorFactory.notNull(waveId, "波次不能为空");
        expandWaveMonsterQuantities(waveId);
        List<GameWaveMonster> rows = listWaveMonsters(waveId);
        if (rows.isEmpty()) {
            return rows;
        }
        rows.sort(Comparator
                .comparingInt((GameWaveMonster r) -> footprintArea(r))
                .thenComparingInt(r -> r.getSort() != null ? r.getSort() : 0));

        boolean[][] board = BattleFormation.emptyBoard();
        for (GameWaveMonster row : rows) {
            int w = row.getFootprintW() != null && row.getFootprintW() > 0 ? row.getFootprintW() : 1;
            int h = row.getFootprintH() != null && row.getFootprintH() > 0 ? row.getFootprintH() : 1;
            w = Math.min(w, BattleFormation.COLS);
            h = Math.min(h, BattleFormation.ROWS);
            int[] pos = BattleFormation.findSymmetricFit(board, w, h);
            if (pos == null) {
                row.setSlotCol(0);
                row.setSlotRow(0);
            } else {
                row.setSlotCol(pos[0]);
                row.setSlotRow(pos[1]);
                BattleFormation.mark(board, pos[0], pos[1], w, h);
            }
            waveMonsterService.updateById(row);
        }
        return listWaveMonsters(waveId);
    }

    /** 把 quantity>1 拆成多条 quantity=1 */
    private void expandWaveMonsterQuantities(String waveId) {
        List<GameWaveMonster> rows = waveMonsterService.find()
                .eq(GameWaveMonster::getWaveId, waveId)
                .orderByAsc(GameWaveMonster::getSort)
                .list();
        for (GameWaveMonster row : rows) {
            int qty = row.getQuantity() != null ? row.getQuantity() : 1;
            if (qty <= 1) {
                if (row.getQuantity() == null || row.getQuantity() != 1) {
                    row.setQuantity(1);
                    waveMonsterService.updateById(row);
                }
                continue;
            }
            Integer baseSort = row.getSort() != null ? row.getSort() : 0;
            Integer firstCol = row.getSlotCol();
            Integer firstRow = row.getSlotRow();
            String monsterId = row.getMonsterId();
            waveMonsterService.removeById(row.getId());
            for (int i = 0; i < qty; i++) {
                GameWaveMonster copy = new GameWaveMonster();
                copy.setWaveId(waveId);
                copy.setMonsterId(monsterId);
                copy.setQuantity(1);
                copy.setSort(baseSort + i);
                if (i == 0) {
                    copy.setSlotCol(firstCol);
                    copy.setSlotRow(firstRow);
                }
                waveMonsterService.save(copy);
            }
        }
    }

    private int footprintArea(GameWaveMonster row) {
        int w = row.getFootprintW() != null && row.getFootprintW() > 0 ? row.getFootprintW() : 1;
        int h = row.getFootprintH() != null && row.getFootprintH() > 0 ? row.getFootprintH() : 1;
        return w * h;
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
                MonsterRank rank = MonsterRank.parse(monster.getRankType());
                row.setRankType(rank.name());
                row.setRankLabel(rank.getLabel());
                int w = monster.getFootprintW() != null && monster.getFootprintW() > 0
                        ? monster.getFootprintW() : rank.getFootprintW();
                int h = monster.getFootprintH() != null && monster.getFootprintH() > 0
                        ? monster.getFootprintH() : rank.getFootprintH();
                row.setFootprintW(w);
                row.setFootprintH(h);
            } else {
                row.setRankType(MonsterRank.NORMAL.name());
                row.setRankLabel(MonsterRank.NORMAL.getLabel());
                row.setFootprintW(1);
                row.setFootprintH(1);
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
