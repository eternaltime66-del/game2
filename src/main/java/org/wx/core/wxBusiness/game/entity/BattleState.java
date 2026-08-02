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

    @JSONField(serialize = false, deserialize = false)
    public boolean isRunning() {
        return STATUS_RUNNING.equals(status);
    }
}
