package org.wx.core.web3unit;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Web3HashCheckResult {

    private boolean success = false;   // 是否校验成功
    private String failMsg;            // 失败原因

    private String hash;               // 交易哈希
    private String from;
    private String to;
    private BigDecimal amount;

    public void fail(String msg) {
        this.success = false;
        this.failMsg = "哈希校验异常: "+msg;
    }

    public void ok() {
        this.success = true;
    }

    public void logJson(){
        System.out.println(JSONObject.toJSONString(this));
    }
}
