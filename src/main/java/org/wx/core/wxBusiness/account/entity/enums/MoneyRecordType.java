package org.wx.core.wxBusiness.account.entity.enums;


import com.alibaba.fastjson2.JSONObject;

/**
 * @author 无心
 * 资金流水类型
 */


public enum MoneyRecordType {
    //充值AiFi点数
    BUY_AGENT("购买节点"),

    BUY_AGENT_REWARD("下级购买节点直推奖"),
    SUB_WITHDRAW("提交提现"),
    FAIL_WITHDRAW("提现失败"),
    SUCCESS_WITHDRAW("提现成功"),
    //系统减少
    Other("其他");

    final String msg;
    MoneyRecordType(String msg) {
        this.msg = msg;
    }

    public String getMsg(){
        return this.msg;
    }


    public static void x(){
        JSONObject json = new JSONObject();
        for (MoneyRecordType type:MoneyRecordType.values()){
            json.put(type.toString(),type.getMsg());
        }
        System.err.println(json);
    }

    public static void main(String[] args) {
        x();
    }


}