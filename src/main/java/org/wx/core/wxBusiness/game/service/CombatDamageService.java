package org.wx.core.wxBusiness.game.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 防御与伤害：实际受到伤害 = 输出伤害 × 输出伤害 / (输出伤害 + 防御)
 * 例：15 伤害、1 防御 → 15 × 15 / 16 = 14.1（保留 1 位小数）
 */
public final class CombatDamageService {

    public static final int DAMAGE_SCALE = 1;

    private CombatDamageService() {
    }

    public static BigDecimal calcReceivedDamage(BigDecimal outputDamage, int defense) {
        if (outputDamage == null || outputDamage.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroDamage();
        }
        BigDecimal output = outputDamage.setScale(DAMAGE_SCALE, RoundingMode.HALF_UP);
        if (defense <= 0) {
            return output;
        }
        return output.multiply(output)
                .divide(output.add(BigDecimal.valueOf(defense)), DAMAGE_SCALE, RoundingMode.HALF_UP);
    }

    public static String formatDamage(BigDecimal damage) {
        if (damage == null) {
            return "0";
        }
        return damage.setScale(DAMAGE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static BigDecimal zeroDamage() {
        return BigDecimal.ZERO.setScale(DAMAGE_SCALE, RoundingMode.HALF_UP);
    }
}
