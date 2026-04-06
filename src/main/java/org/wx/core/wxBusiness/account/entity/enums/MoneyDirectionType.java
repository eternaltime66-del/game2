package org.wx.core.wxBusiness.account.entity.enums;

/**
 * @author 无心
 * 资金流水方向枚举
 */

public enum MoneyDirectionType {
    /**
     * 增
     */
    Increase("增加"),Reduce("减少");

    String msg;

    MoneyDirectionType(String msg) {
        this.msg = msg;
    }
}
