package org.wx.core.wxBusiness.account.entity.enums;


import com.alibaba.fastjson2.JSONObject;

/**
 * @author 无心
 * 资金流水类型
 */


public enum MoneyRecordType {
    SUB_WITHDRAW("提交提现"),
    FAIL_WITHDRAW("提现失败"),
    SUCCESS_WITHDRAW("提现成功"),
    TRS_OUT("转出"),
    TRS_INTO("转入"),
    WEB3_RECHARGE("链上充值"),
    Other("其他");

    final String msg;
    MoneyRecordType(String msg) {
        this.msg = msg;
    }

    public String getMsg(){
        return this.msg;
    }

    /** 手动执行，输出枚举 JSON 供前端使用 */
    public static void jsonOut() {
        JSONObject json = new JSONObject();
        for (MoneyRecordType type : MoneyRecordType.values()) {
            json.put(type.toString(), type.getMsg());
        }
        System.err.println(json);
    }

    public static void main(String[] args) {
        jsonOut();
    }
}
