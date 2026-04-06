package org.wx.core.wxBusiness.account.entity;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBase.unit.JsonUnit;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.account.entity.enums.MoneyDirectionType;
import org.wx.core.wxBusiness.account.entity.enums.MoneyRecordType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MoneyRecord 实体类
 * @author 无心
 * @date 2026-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_money_record")
public class MoneyRecord extends WxBaseEntity<MoneyRecord> {

    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 用户id
     */
    private String uid;

    /**
     * 类型
     */
    private MoneyRecordType type;

    /**
     * 变动前
     */
    private BigDecimal beforeBalance;

    /**
     * 变动后
     */
    private BigDecimal afterBalance;

    /**
     * 数额
     */
    private BigDecimal amount;

    /**
     * 方向 增加/减少
     */
    private MoneyDirectionType direction;

    /**
     * 货币
     */
    private String coin;

    /**
     * 钱包类型
     */
    private String walletType;

    /**
     * 钱包id
     */
    private String walletId;

    /**
     * 更多数据
     */
    private String more;

    /**
     * 链上支付金额
     */
    private BigDecimal webAmount;

    /**
     * 链上交易Hash
     */
    private String hash;


    // ===== 新增 Web3 流水专属构建方法 =====
    /**
     * 构建 Web3 钱包流水记录（静态工厂方法）
     * @param uid          用户ID
     * @param type         流水类型
     * @param amount       变动金额
     * @param direction    变动方向（增加/减少）
     * @return Web3 专属流水对象
     */
    public static MoneyRecord web3(
            String uid,
            MoneyRecordType type,
            BigDecimal amount,
            MoneyDirectionType direction
    ) {
        MoneyRecord record = new MoneyRecord();
        record.id = type.toString() + "_" + WordUnit.nowId(16, 1);
        // 基础流水字段
        record.setUid(uid);
        record.setType(type); // 转换为字符串存储（适配数据库字段）
        record.setAmount(amount);
        record.setDirection(direction); // 方向转字符串
        record.setWalletType("Web3"); // 标记为Web3钱包流水
        return record;
    }

    public static MoneyRecord point(String uid, MoneyRecordType type, BigDecimal beforeBalance, BigDecimal afterBalance, BigDecimal amount, MoneyDirectionType direction, PointWallet wallet) {
        MoneyRecord moneyRecord = new MoneyRecord();
        moneyRecord.id = type.toString() + "_" + WordUnit.nowId(16, 1);
        moneyRecord.uid = uid;
        moneyRecord.type = type;
        moneyRecord.beforeBalance = beforeBalance;
        moneyRecord.afterBalance = afterBalance;
        moneyRecord.amount = amount;
        moneyRecord.direction = direction;
        moneyRecord.coin = wallet.getCoin().toString();
        moneyRecord.walletId = wallet.getId();
        moneyRecord.setWalletType("Point");
        return moneyRecord;
    }

    @Transactional(rollbackFor = Exception.class)
    public JsonUnit loadMore() {
        if (this.more==null || this.more.isEmpty()){
            this.more="{}";
        }
        JSONObject jsonObject = JSONObject.parseObject(this.more);
        return Wx.json(jsonObject);
    }
    /**
     * 用户id
     */
    private String address;

}
