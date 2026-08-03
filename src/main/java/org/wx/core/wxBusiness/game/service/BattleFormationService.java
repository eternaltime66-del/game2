package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.BattleFormation;
import org.wx.core.wxBusiness.game.entity.BattleUnit;
import org.wx.core.wxBusiness.game.entity.GameHero;
import org.wx.core.wxBusiness.game.entity.GameMonster;
import org.wx.core.wxBusiness.game.entity.enums.MonsterRank;

import java.util.List;

/**
 * 战斗站位：配置在哪就在哪，战斗中不自动挪位/补位。
 * 仅当波次未写站位时，开战前做一次初始落位。
 */
@Service
public class BattleFormationService {

    public void placeHero(BattleUnit hero) {
        placeHero(hero, null);
    }

    public void placeHero(BattleUnit hero, GameHero template) {
        if (hero == null) {
            return;
        }
        hero.setRankType("HERO");
        hero.applyFootprint(BattleFormation.HERO_FOOTPRINT_W, BattleFormation.HERO_FOOTPRINT_H);
        int col = template != null && template.getSlotCol() != null ? template.getSlotCol() : 1;
        int row = template != null && template.getSlotRow() != null ? template.getSlotRow() : 0;
        hero.setSlotCol(col);
        hero.setSlotRow(row);
        clampToBoard(hero);
    }

    public void placeMonsters(List<BattleUnit> monsters) {
        if (monsters == null || monsters.isEmpty()) {
            return;
        }
        boolean[][] board = BattleFormation.emptyBoard();

        // 1) 已配置站位：原样保留，不因冲突挪走
        for (BattleUnit unit : monsters) {
            ensureFootprint(unit);
            if (unit.getSlotCol() == null || unit.getSlotRow() == null) {
                continue;
            }
            clampToBoard(unit);
            BattleFormation.mark(board, unit.getSlotCol(), unit.getSlotRow(),
                    unit.getFootprintW(), unit.getFootprintH());
        }

        // 2) 未配置：仅开战前找空位落一次，之后也不再移动
        for (BattleUnit unit : monsters) {
            if (unit.getSlotCol() != null && unit.getSlotRow() != null) {
                continue;
            }
            ensureFootprint(unit);
            int w = unit.getFootprintW();
            int h = unit.getFootprintH();
            int[] pos = BattleFormation.findFirstFit(board, w, h);
            if (pos == null) {
                unit.setSlotCol(0);
                unit.setSlotRow(0);
            } else {
                unit.setSlotCol(pos[0]);
                unit.setSlotRow(pos[1]);
                BattleFormation.mark(board, pos[0], pos[1], w, h);
            }
        }
    }

    public void applyMonsterTemplate(BattleUnit unit, GameMonster monster) {
        if (unit == null) {
            return;
        }
        if (monster == null) {
            unit.setRankType(MonsterRank.NORMAL.name());
            unit.applyFootprint(1, 1);
            return;
        }
        MonsterRank rank = MonsterRank.parse(monster.getRankType());
        unit.setRankType(rank.name());
        int w = monster.getFootprintW() != null && monster.getFootprintW() > 0
                ? monster.getFootprintW() : rank.getFootprintW();
        int h = monster.getFootprintH() != null && monster.getFootprintH() > 0
                ? monster.getFootprintH() : rank.getFootprintH();
        unit.applyFootprint(w, h);
    }

    private void ensureFootprint(BattleUnit unit) {
        if (unit.getFootprintW() == null || unit.getFootprintW() < 1
                || unit.getFootprintH() == null || unit.getFootprintH() < 1) {
            MonsterRank rank = MonsterRank.parse(unit.getRankType());
            unit.applyFootprint(rank.getFootprintW(), rank.getFootprintH());
        }
    }

    /** 把左上角压进棋盘，不改变占地尺寸、不另找空位 */
    private void clampToBoard(BattleUnit unit) {
        int w = Math.max(1, unit.getFootprintW() != null ? unit.getFootprintW() : 1);
        int h = Math.max(1, unit.getFootprintH() != null ? unit.getFootprintH() : 1);
        w = Math.min(w, BattleFormation.COLS);
        h = Math.min(h, BattleFormation.ROWS);
        unit.applyFootprint(w, h);
        int col = unit.getSlotCol() != null ? unit.getSlotCol() : 0;
        int row = unit.getSlotRow() != null ? unit.getSlotRow() : 0;
        col = Math.max(0, Math.min(col, BattleFormation.COLS - w));
        row = Math.max(0, Math.min(row, BattleFormation.ROWS - h));
        unit.setSlotCol(col);
        unit.setSlotRow(row);
    }
}
