package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.mapper.GameStageMapper;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameLevelService extends WxServiceImpl<GameStageMapper, GameStage> {

    @Resource
    private GameModeGroupService modeGroupService;
    @Resource
    private GameChapterService chapterService;
    @Resource
    private GameStageGroupService stageGroupService;
    @Resource
    private GameWaveService waveService;
    @Resource
    private GameWaveMonsterService waveMonsterService;

    @Transactional(rollbackFor = Exception.class)
    public void saveModeGroup(GameModeGroup entity) {
        ErrorFactory.notNull(entity.getCode(), "编码不能为空");
        ErrorFactory.notNull(entity.getName(), "名称不能为空");
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        modeGroupService.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void     saveChapter(GameChapter entity) {
        ErrorFactory.notNull(entity.getGroupId(), "模式分组不能为空");
        ErrorFactory.notNull(entity.getCode(), "编码不能为空");
        ErrorFactory.notNull(entity.getName(), "名称不能为空");
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        chapterService.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void     saveStageGroup(GameStageGroup entity) {
        ErrorFactory.notNull(entity.getChapterId(), "大关卡不能为空");
        ErrorFactory.notNull(entity.getGroupNo(), "组编号不能为空");
        ErrorFactory.notNull(entity.getName(), "名称不能为空");
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        stageGroupService.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveStage(GameStage entity) {
        ErrorFactory.notNull(entity.getStageGroupId(), "小关卡组不能为空");
        ErrorFactory.notNull(entity.getStageNo(), "关卡序号不能为空");
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        this.saveOrUpdate(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeModeGroup(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        List<GameChapter> chapters = chapterService.find()
                .eq(GameChapter::getGroupId, id)
                .list();
        for (GameChapter chapter : chapters) {
            removeChapter(chapter.getId());
        }
        modeGroupService.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeChapter(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        List<GameStageGroup> groups = stageGroupService.find()
                .eq(GameStageGroup::getChapterId, id)
                .list();
        for (GameStageGroup group : groups) {
            removeStageGroup(group.getId());
        }
        chapterService.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeStageGroup(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        List<GameStage> stages = this.find()
                .eq(GameStage::getStageGroupId, id)
                .list();
        for (GameStage stage : stages) {
            removeStage(stage.getId());
        }
        stageGroupService.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeStage(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        deleteWavesByStageId(id);
        this.removeById(id);
    }

    private void deleteWavesByStageId(String stageId) {
        List<GameWave> waves = waveService.find()
                .eq(GameWave::getStageId, stageId)
                .list();
        for (GameWave wave : waves) {
            waveMonsterService.remove(new LambdaQueryWrapper<GameWaveMonster>()
                    .eq(GameWaveMonster::getWaveId, wave.getId()));
            waveService.removeById(wave.getId());
        }
    }

    public List<GameStage> listStagesByChapterId(String chapterId) {
        List<GameStageGroup> groups = stageGroupService.find()
                .eq(GameStageGroup::getChapterId, chapterId)
                .eq(GameStageGroup::getEnabled, 1)
                .orderByAsc(GameStageGroup::getSort)
                .orderByAsc(GameStageGroup::getGroupNo)
                .list();
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> groupNoMap = groups.stream()
                .collect(Collectors.toMap(GameStageGroup::getId, GameStageGroup::getGroupNo));
        List<String> groupIds = groups.stream().map(GameStageGroup::getId).toList();
        List<GameStage> stages = this.find()
                .in(GameStage::getStageGroupId, groupIds)
                .eq(GameStage::getEnabled, 1)
                .orderByAsc(GameStage::getSort)
                .orderByAsc(GameStage::getStageNo)
                .list();
        for (GameStage stage : stages) {
            stage.fillDisplayCode(groupNoMap.get(stage.getStageGroupId()));
        }
        stages.sort(Comparator
                .comparing((GameStage s) -> s.getGroupNo() == null ? 0 : s.getGroupNo())
                .thenComparing(s -> s.getStageNo() == null ? 0 : s.getStageNo()));
        return stages;
    }

    public LevelTree buildLevelTree() {
        LevelTree tree = new LevelTree();
        List<GameModeGroup> modeGroups = modeGroupService.find()
                .orderByAsc(GameModeGroup::getSort)
                .list();
        for (GameModeGroup modeGroup : modeGroups) {
            ModeGroupNode modeNode = new ModeGroupNode();
            modeNode.setModeGroup(modeGroup);
            List<GameChapter> chapters = chapterService.find()
                    .eq(GameChapter::getGroupId, modeGroup.getId())
                    .orderByAsc(GameChapter::getSort)
                    .list();
            for (GameChapter chapter : chapters) {
                ChapterNode chapterNode = new ChapterNode();
                chapterNode.setChapter(chapter);
                List<GameStageGroup> stageGroups = stageGroupService.find()
                        .eq(GameStageGroup::getChapterId, chapter.getId())
                        .orderByAsc(GameStageGroup::getSort)
                        .orderByAsc(GameStageGroup::getGroupNo)
                        .list();
                for (GameStageGroup stageGroup : stageGroups) {
                    StageGroupNode groupNode = new StageGroupNode();
                    groupNode.setStageGroup(stageGroup);
                    List<GameStage> stages = this.find()
                            .eq(GameStage::getStageGroupId, stageGroup.getId())
                            .orderByAsc(GameStage::getSort)
                            .orderByAsc(GameStage::getStageNo)
                            .list();
                    for (GameStage stage : stages) {
                        stage.fillDisplayCode(stageGroup.getGroupNo());
                    }
                    groupNode.setStages(stages);
                    chapterNode.getStageGroups().add(groupNode);
                }
                modeNode.getChapters().add(chapterNode);
            }
            tree.getModeGroups().add(modeNode);
        }
        return tree;
    }

    @Data
    public static class LevelTree {
        private List<ModeGroupNode> modeGroups = new ArrayList<>();
    }

    @Data
    public static class ModeGroupNode {
        private GameModeGroup modeGroup;
        private List<ChapterNode> chapters = new ArrayList<>();
    }

    @Data
    public static class ChapterNode {
        private GameChapter chapter;
        private List<StageGroupNode> stageGroups = new ArrayList<>();
    }

    @Data
    public static class StageGroupNode {
        private GameStageGroup stageGroup;
        private List<GameStage> stages = new ArrayList<>();
    }
}
