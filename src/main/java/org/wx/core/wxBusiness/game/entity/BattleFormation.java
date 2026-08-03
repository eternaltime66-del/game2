package org.wx.core.wxBusiness.game.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 战斗站位：双方各 4 列 × 3 行 = 12 格。
 * <pre>
 * 敌方（屏幕上）：
 *   3A 3B 3C 3D   ← 行2，后排
 *   2A 2B 2C 2D   ← 行1
 *   1A 1B 1C 1D   ← 行0，前排（靠近己方）
 *
 * 己方镜像：行0 在上靠近敌方为前排，行2 在下为后排。
 * </pre>
 * 列：0=A … 3=D。单位固定站位，战斗中不挪位。
 */
public final class BattleFormation {

    public static final int COLS = 4;
    public static final int ROWS = 3;
    public static final int SLOT_COUNT = COLS * ROWS;

    /** 主角占地：横 2 × 竖 1 */
    public static final int HERO_FOOTPRINT_W = 2;
    public static final int HERO_FOOTPRINT_H = 1;

    private BattleFormation() {
    }

    public static int slotIndex(int col, int row) {
        return row * COLS + col;
    }

    /** 展示用行号 1–3（1=前排） */
    public static int displayRow(int slotRow) {
        return slotRow + 1;
    }

    /** 展示用列号 A–D */
    public static char displayCol(int slotCol) {
        return (char) ('A' + Math.max(0, Math.min(COLS - 1, slotCol)));
    }

    public static boolean inBounds(int col, int row, int w, int h) {
        return col >= 0 && row >= 0 && col + w <= COLS && row + h <= ROWS;
    }

    public static boolean occupiesRow(BattleUnit unit, int row) {
        if (unit == null || unit.getSlotRow() == null) {
            return false;
        }
        int h = unit.getFootprintH() != null && unit.getFootprintH() > 0 ? unit.getFootprintH() : 1;
        int top = unit.getSlotRow();
        return row >= top && row < top + h;
    }

    public static boolean occupiesCol(BattleUnit unit, int col) {
        if (unit == null || unit.getSlotCol() == null) {
            return false;
        }
        int w = unit.getFootprintW() != null && unit.getFootprintW() > 0 ? unit.getFootprintW() : 1;
        int left = unit.getSlotCol();
        return col >= left && col < left + w;
    }

    /** 占用格子列表（左上角起点） */
    public static List<int[]> occupiedCells(int col, int row, int w, int h) {
        List<int[]> cells = new ArrayList<>();
        for (int r = row; r < row + h; r++) {
            for (int c = col; c < col + w; c++) {
                cells.add(new int[]{c, r});
            }
        }
        return cells;
    }

    public static boolean overlaps(boolean[][] occupied, int col, int row, int w, int h) {
        if (!inBounds(col, row, w, h)) {
            return true;
        }
        for (int r = row; r < row + h; r++) {
            for (int c = col; c < col + w; c++) {
                if (occupied[r][c]) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void mark(boolean[][] occupied, int col, int row, int w, int h) {
        if (!inBounds(col, row, w, h)) {
            return;
        }
        for (int r = row; r < row + h; r++) {
            for (int c = col; c < col + w; c++) {
                occupied[r][c] = true;
            }
        }
    }

    public static boolean[][] emptyBoard() {
        return new boolean[ROWS][COLS];
    }

    /**
     * 仅用于「未配置站位」的初始落位：从前排到后排、从左到右找空位。
     * 已配置站位不会走这里，也不会被挪动。
     */
    public static int[] findFirstFit(boolean[][] occupied, int w, int h) {
        for (int row = 0; row <= ROWS - h; row++) {
            for (int col = 0; col <= COLS - w; col++) {
                if (!overlaps(occupied, col, row, w, h)) {
                    return new int[]{col, row};
                }
            }
        }
        return null;
    }

    /**
     * 对称优先落位：前排优先，同排优先居中（小体型更靠前由调用方排序保证）。
     */
    public static int[] findSymmetricFit(boolean[][] occupied, int w, int h) {
        List<Integer> colOrder = centeredColOrder(w);
        for (int row = 0; row <= ROWS - h; row++) {
            for (int col : colOrder) {
                if (!overlaps(occupied, col, row, w, h)) {
                    return new int[]{col, row};
                }
            }
        }
        return findFirstFit(occupied, w, h);
    }

    /** 以棋盘中线为优先的列起点顺序 */
    public static List<Integer> centeredColOrder(int w) {
        int maxCol = COLS - Math.max(1, w);
        int ideal = Math.max(0, (COLS - w) / 2);
        List<Integer> order = new ArrayList<>();
        order.add(ideal);
        for (int d = 1; d <= COLS; d++) {
            int left = ideal - d;
            int right = ideal + d;
            if (left >= 0 && left <= maxCol) {
                order.add(left);
            }
            if (right >= 0 && right <= maxCol && right != left) {
                order.add(right);
            }
        }
        return order;
    }

    /**
     * 前排：从行0(1排)往行2(3排)扫，第一排有存活单位的格子即为当前前排。
     */
    public static List<BattleUnit> unitsOnFrontRow(List<BattleUnit> units) {
        return unitsOnScannedRow(units, true);
    }

    /**
     * 后排：从行2(3排)往行0(1排)扫，第一排有存活单位的格子即为当前后排。
     * （不是「非前排的全部」，而是单行）
     */
    public static List<BattleUnit> unitsOnBackRow(List<BattleUnit> units) {
        return unitsOnScannedRow(units, false);
    }

    /** @deprecated 使用 {@link #unitsOnBackRow} */
    @Deprecated
    public static List<BattleUnit> unitsOnBackRows(List<BattleUnit> units) {
        return unitsOnBackRow(units);
    }

    private static List<BattleUnit> unitsOnScannedRow(List<BattleUnit> units, boolean fromFront) {
        List<BattleUnit> alive = units == null ? List.of()
                : units.stream().filter(BattleUnit::isAlive).toList();
        if (alive.isEmpty()) {
            return List.of();
        }
        boolean anySlot = alive.stream().anyMatch(u -> u.getSlotRow() != null);
        if (!anySlot) {
            return alive;
        }
        if (fromFront) {
            for (int row = 0; row < ROWS; row++) {
                List<BattleUnit> onRow = unitsOccupyingRow(alive, row);
                if (!onRow.isEmpty()) {
                    return onRow;
                }
            }
        } else {
            for (int row = ROWS - 1; row >= 0; row--) {
                List<BattleUnit> onRow = unitsOccupyingRow(alive, row);
                if (!onRow.isEmpty()) {
                    return onRow;
                }
            }
        }
        return List.of();
    }

    private static List<BattleUnit> unitsOccupyingRow(List<BattleUnit> alive, int row) {
        return alive.stream()
                .filter(u -> occupiesRow(u, row))
                .sorted(Comparator.comparingInt(u -> u.getSlotCol() != null ? u.getSlotCol() : 0))
                .toList();
    }
}
