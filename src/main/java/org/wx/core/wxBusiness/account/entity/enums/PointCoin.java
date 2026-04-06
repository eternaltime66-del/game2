package org.wx.core.wxBusiness.account.entity.enums;

public enum PointCoin {
    USDT(8);

    public final Integer decimals;
    PointCoin(int decimals) {
        this.decimals = decimals;
    }
}
