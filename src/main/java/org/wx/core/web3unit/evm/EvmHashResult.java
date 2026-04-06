package org.wx.core.web3unit.evm;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.TypeReference;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;
import org.wx.core.web3unit.BigIntegerDecUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Web3 交易哈希解析结果
 * 安全、强类型、高性能版本
 *
 * @author 无心
 */
@Data
@Slf4j
public class EvmHashResult {

//    public static class TradeEvmVo extends EvmEventVo{
//        EvmEventParam address = new EvmEventParam(EvmFun);
//        EvmEventParam outAmount;
//        EvmEventParam intoAmount;
//        EvmEventParam boyOrSell;
//
//        HashMap<String,EvmEventParam> map = new HashMap<>();
//
//
//        public BigDecimal getPrice;
//
//
//    }
//
//    @Data
//    public static class EvmEventVo{
//        String eventName;
//        EvmEventParamCenter center;
//    }
//
//    public static enum EventParamType{
//        Topic,
//        EventData;
//    }
//
//    @Data
//    public static class EvmEventSuperParam{
//        Integer index;
//        EventParamType type;
//    }
//
//    @Data
//    public static class EvmEventParam extends EvmEventSuperParam{
//        TypeReference<?> intoParam;
//        Class<?> outParam;
//
//        public EvmEventParam(TypeReference<?> intoParam,Class<?> outParam,BigInteger index){
//            this.intoParam = intoParam;
//            this.outParam = outParam;
//        }
//    }
//    @Data
//    public static class EvmEventParamCenter{
//
//        ArrayList<EvmEventParam<?,?>> paramArrayList;
//
//        public static EvmEventParamCenter build(){
//            return new EvmEventParamCenter();
//        }
//
//        public EvmEventParamCenter addParam(EvmEventParam<?,?> param){
//            this.getParamArrayList().add(param);
//            return this;
//        }
//
//    }



    private String from;
    private String to;
    private BigInteger val;

    private List<String> eventTopics;
    private String eventData;
    private List<Log> logs;

    public String topice(Integer index){
        return "0x" + this.eventTopics.get(index).substring(26 * 1);
    }

    public String getEventDataItem(Integer index){
        String e = this.eventData.substring(2);
        return e.substring((index-1)*64, index*64);
    }

    public BigInteger getBigInteger(Integer index){
        return new BigInteger(getEventDataItem(index), 16);

    }

    public BigDecimal getBigDecimal(Integer index,Integer decimals){
        return BigIntegerDecUtils.decimals(getBigInteger(index),decimals);
    }
    public BigDecimal getBigDecimal18(Integer index){
        return getBigDecimal(index,18);
    }

    public Boolean getBoolean(Integer index){
        return getBigInteger(index).equals(BigInteger.ONE);
    }

    public EvmHashResult(String from, String to, BigInteger val, List<Log> logs) {
        this.from = from;
        this.to = to;
        this.val = val;
        this.logs = logs;
    }

    public EvmHashResult() {}

    // ============================================================
    //         事件参数解析（index 基于 0）
    // ============================================================

    /** 获取事件中的第 index 个地址（自动去 padding） */
    public String getDataAddress(int index) {
        String hex = getHex32(index);
        if (hex == null) return null;
        return "0x" + hex.substring(24 * 1);   // 保留地址后 40 位 hex
    }

    /** 获取事件中的第 index 个 uint256 */
    public BigInteger getDataBigInteger(int index) {
        String hex = getHex32(index);
        if (hex == null) return null;
        return Numeric.toBigInt(hex);
    }

    /** 获取 eventData 中第 index 个 32byte 数据 */
    private String getHex32(int index) {
        if (eventData == null || eventData.length() < 2) {
            log.error("eventData 为空");
            return null;
        }

        int start = 2 + index * 64;
        int end   = start + 64;

        if (end > eventData.length()) {
            log.error("eventData length out of range index={}", index);
            return null;
        }

        return eventData.substring(start, end);
    }

    // ============================================================
    //         Log 解析（取 logs[index]）
    // ============================================================

    public EvmHashResult getLog(int index) {
        if (logs == null || logs.size() <= index) {
            log.error("log 下标越界 index={}", index);
            return null;
        }

        Log logEntry = logs.get(index);

        EvmHashResult r = new EvmHashResult(from, to, val, logs);
        r.setEventData(logEntry.getData());
        r.setEventTopics(logEntry.getTopics());

        return r;
    }

    // ============================================================
    //       精度转换工具
    // ============================================================

    /** Wei → 精度金额 */
    public static BigDecimal accuracyReversal(BigInteger wei, int accuracy) {
        return new BigDecimal(wei)
                .divide(BigDecimal.TEN.pow(accuracy), accuracy, RoundingMode.DOWN);
    }

    /** 金额 → Wei */
    public static BigInteger setAccuracy(BigDecimal amount, int accuracy) {
        return amount.multiply(BigDecimal.TEN.pow(accuracy))
                .setScale(0, RoundingMode.DOWN)
                .toBigInteger();
    }

    /** 从 topic 中解析地址（indexed 参数） */
    public static String topicToAddress(String topic) {
        if (topic == null || topic.length() < 66) {
            log.error("topic 长度不正确: {}", topic);
            return null;
        }
        // 去掉 padding，仅保留后 40 位（20 bytes）
        return "0x" + topic.substring(topic.length() - 40);
    }

    // ============================================================
    // 调试
    // ============================================================
    public static void main(String[] args) {
        // 测试方法
    }
}
