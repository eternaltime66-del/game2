package org.wx.core.web3unit;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * BigInteger 精度转换工具（极致简洁：decimals(18)/dec18()）
 * 无侵入、不继承、仅通过静态导入实现"伪链式调用"
 * 核心优势：
 * 1. 语法：bigInteger.decimals(18) → 直接转18位精度BigDecimal
 * 2. 兼容所有BigInteger对象，无需修改原有代码
 * 3. 内置空值处理、异常防护
 */
public final class BigIntegerDecUtils {
    // 区块链场景默认舍入模式（向下取整，避免四舍五入误差）
    private static final RoundingMode DEFAULT_ROUND = RoundingMode.FLOOR;
    
    // 常用精度常量（可直接复用）
    public static final int DEC_18 = 18;
    public static final int DEC_6 = 6;
    public static final int DEC_0 = 0;

    // 私有化构造方法，禁止实例化
    private BigIntegerDecUtils() {}

    // ==================== 核心极简方法 ====================
    /**
     * 极简语法：BigInteger → BigDecimal（指定精度）
     * 使用：bigInteger.decimals(18)
     */
    public static BigDecimal decimals(BigInteger bigInteger, int decimals) {
        if (bigInteger == null) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal tenPow = BigDecimal.TEN.pow(decimals);
            return new BigDecimal(bigInteger)
                    .divide(tenPow, decimals, DEFAULT_ROUND);
        } catch (ArithmeticException e) {
            // 处理极端情况（如精度过大导致的溢出）
            System.out.println("溢出了");
            return BigDecimal.ZERO;
        }
    }

    /**
     * 快捷语法：默认18位精度（区块链最常用）
     * 使用：bigInteger.dec18()
     */
    public static BigDecimal decimals18(BigInteger bigInteger) {
        return decimals(bigInteger, DEC_18);
    }

    /**
     * 快捷语法：6位精度（适配USDT/ERC20旧版本）
     * 使用：bigInteger.dec6()
     */
    public static BigDecimal decimals6(BigInteger bigInteger) {
        return decimals(bigInteger, DEC_6);
    }

    // ==================== 反向转换（实际单位 → Wei） ====================
    /**
     * 极简反向转换：BigDecimal(实际单位) → BigInteger(Wei)
     * 使用：BigIntegerDecUtils.toWei(actualAmount, 18)
     */
    public static BigInteger toWei(BigDecimal actualAmount, int decimals) {
        if (actualAmount == null) {
            return BigInteger.ZERO;
        }
        try {
            BigDecimal weiValue = actualAmount.multiply(BigDecimal.TEN.pow(decimals))
                    .setScale(0, DEFAULT_ROUND);
            return weiValue.toBigIntegerExact();
        } catch (ArithmeticException e) {
            return BigInteger.ZERO;
        }
    }

    /**
     * 快捷反向转换：18位精度
     * 使用：BigIntegerDecUtils.toWei18(actualAmount)
     */
    public static BigInteger toWei18(BigDecimal actualAmount) {
        return toWei(actualAmount, DEC_18);
    }
}