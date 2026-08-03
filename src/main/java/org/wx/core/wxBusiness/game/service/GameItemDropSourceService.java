package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameItemDropSourceService {

    @Resource
    private GameMonsterDropService gameMonsterDropService;
    @Resource
    private GameWaveMonsterService waveMonsterService;
    @Resource
    private GameWaveService waveService;
    @Resource
    private GameLevelService levelService;
    @Resource
    private GameStageGroupService stageGroupService;
    @Resource
    private GameMonsterService monsterService;

    public List<ItemDropSourceVo> listByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        List<GameMonsterDrop> drops = gameMonsterDropService.find()
                .eq(GameMonsterDrop::getItemId, itemId)
                .eq(GameMonsterDrop::getEnabled, 1)
                .orderByAsc(GameMonsterDrop::getSort)
                .list();
        if (drops.isEmpty()) {
            return List.of();
        }

        Map<String, GameMonsterDrop> dropByMonster = new LinkedHashMap<>();
        for (GameMonsterDrop drop : drops) {
            dropByMonster.putIfAbsent(drop.getMonsterId(), drop);
        }

        List<GameWaveMonster> waveMonsters = waveMonsterService.find()
                .in(GameWaveMonster::getMonsterId, dropByMonster.keySet())
                .list();
        if (waveMonsters.isEmpty()) {
            return List.of();
        }

        Set<String> waveIds = waveMonsters.stream()
                .map(GameWaveMonster::getWaveId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, GameWave> waveMap = waveService.listByIds(waveIds).stream()
                .collect(Collectors.toMap(GameWave::getId, w -> w, (a, b) -> a));

        Set<String> stageIds = waveMap.values().stream()
                .map(GameWave::getStageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, GameStage> stageMap = levelService.listByIds(stageIds).stream()
                .collect(Collectors.toMap(GameStage::getId, s -> s, (a, b) -> a));

        Set<String> groupIds = stageMap.values().stream()
                .map(GameStage::getStageGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Integer> groupNoMap = stageGroupService.listByIds(groupIds).stream()
                .collect(Collectors.toMap(GameStageGroup::getId, GameStageGroup::getGroupNo, (a, b) -> a));

        Set<String> monsterIds = waveMonsters.stream()
                .map(GameWaveMonster::getMonsterId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, GameMonster> monsterMap = monsterService.listByIds(monsterIds).stream()
                .collect(Collectors.toMap(GameMonster::getId, m -> m, (a, b) -> a));

        Set<String> seen = new LinkedHashSet<>();
        List<ItemDropSourceVo> result = new ArrayList<>();
        for (GameWaveMonster wm : waveMonsters) {
            GameWave wave = waveMap.get(wm.getWaveId());
            if (wave == null || wave.getStageId() == null) {
                continue;
            }
            GameStage stage = stageMap.get(wave.getStageId());
            if (stage == null || !Integer.valueOf(1).equals(stage.getEnabled())) {
                continue;
            }
            String key = stage.getId() + ":" + wm.getMonsterId();
            if (!seen.add(key)) {
                continue;
            }
            GameMonsterDrop drop = dropByMonster.get(wm.getMonsterId());
            if (drop == null) {
                continue;
            }
            stage.fillDisplayCode(groupNoMap.get(stage.getStageGroupId()));
            GameMonster monster = monsterMap.get(wm.getMonsterId());

            ItemDropSourceVo vo = new ItemDropSourceVo();
            vo.setStageId(stage.getId());
            vo.setDisplayCode(stage.getDisplayCode());
            vo.setStageName(stage.getName());
            vo.setMonsterId(wm.getMonsterId());
            vo.setMonsterName(monster != null ? monster.getName() : "怪物");
            vo.setDropRate(drop.getDropRate());
            vo.setMinQty(drop.getMinQty());
            vo.setMaxQty(drop.getMaxQty());
            result.add(vo);
        }

        result.sort((a, b) -> {
            int[] aa = parseDisplayCode(a.getDisplayCode());
            int[] bb = parseDisplayCode(b.getDisplayCode());
            if (aa[0] != bb[0]) {
                return Integer.compare(aa[0], bb[0]);
            }
            if (aa[1] != bb[1]) {
                return Integer.compare(aa[1], bb[1]);
            }
            return String.valueOf(a.getMonsterName()).compareTo(String.valueOf(b.getMonsterName()));
        });
        return result;
    }

    private static int[] parseDisplayCode(String displayCode) {
        if (displayCode == null || !displayCode.contains("-")) {
            return new int[]{999, 999};
        }
        String[] parts = displayCode.split("-", 2);
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException ex) {
            return new int[]{999, 999};
        }
    }
}
