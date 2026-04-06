package org.wx.core.wxBusiness.account.entity.enums;

/**
*@author 无心
*@date ${DATE}
*@msg 商家用户状态
*@demo ${NAME}
*/
public enum RechargeStateEnum {
    Wait("等待处理"),
    Success("成功"),
    Error("充值成功,系统异常"),
    Fail("失败");

    public String msg;
    RechargeStateEnum(String msg) {
        this.msg = msg;
    }
}
