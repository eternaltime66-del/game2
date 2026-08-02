package org.wx.core.wxBusiness.game.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.math.BigDecimal;
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

    /** 本场战斗累计掉落（胜利后发放到仓库） */
    private List<BattleLootEntry> lootAccumulated = new ArrayList<>();

    /** 战利品是否已发放到仓库 */
    private Boolean lootGranted;

    /** 开战时主角装备的物品 ID（用于扳机） */
    private List<String> heroEquippedItemIds = new ArrayList<>();

    /** 本场累计受到伤害（主角） */
    private BigDecimal heroAccumulatedDamage = BigDecimal.ZERO;

    /** 本场累计恢复生命（主角） */
    private BigDecimal heroAccumulatedHeal = BigDecimal.ZERO;

    /** 本场累计攻击次数（主角） */
    private Integer heroAttackCount = 0;

    @JSONField(serialize = false, deserialize = false)
    public boolean isRunning() {
        return STATUS_RUNNING.equals(status);
    }
}
