package org.wx.core.wxBusiness.game.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BattleState {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_WIN = "WIN";
    public static final String STATUS_LOSE = "LOSE";

    private String battleId;

    private String stageId;

    private String stageName;

    private String displayCode;

    private String uid;

    private String status;

    /** 当前波次（1-based） */
    private Integer currentWave;

    private Integer totalWaves;

    /** 全场统一行动条增速（每 tick） */
    private Integer actionTickGain;

    private List<BattleUnit> units = new ArrayList<>();

    private List<BattleLog> logs = new ArrayList<>();

    /** 全场总轴：每推进一次行动条 tick +1 */
    private int timeline;

    /** 扳机槽运行时计数 */
    private BattleTriggerCounters triggerCounters;

    /** 本场战斗累计掉落（胜利后发放到仓库） */
    private List<BattleLootEntry> lootAccumulated = new ArrayList<>();

    /** 战利品是否已发放到仓库 */
    private Boolean lootGranted;

    /** 主角已装备物品 ID（扳机槽来源） */
    private List<String> heroEquippedItemIds = new ArrayList<>();

    /** 本场消耗型武器物品 ID（非消耗型为 null） */
    private String consumableWeaponItemId;

    /** 消耗型武器剩余使用次数 */
    private Integer weaponUsesLeft;

    @JSONField(serialize = false, deserialize = false)
    public boolean isRunning() {
        return STATUS_RUNNING.equals(status);
    }

    /** 推进总轴（每次行动条 tick 调用一次） */
    public void advanceTimeline() {
        timeline++;
    }

    public BattleLog appendLog(BattleLog log) {
        if (log == null) {
            return null;
        }
        log.setAxis(timeline);
        logs.add(log);
        return log;
    }

    public void appendLogs(List<BattleLog> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (BattleLog log : batch) {
            appendLog(log);
        }
    }
}
