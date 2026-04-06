package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Web3RunWatch 实体类
 * @author 无心
 * @date 2026-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_web3_run_watch")
public class Web3RunWatch extends WxBaseEntity<Web3RunWatch> {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 执行人地址
     */
    private String actionAddress;

    /**
     * 合约地址
     */
    private String contractAddress;

    /**
     * 合约执行方法
     */
    private String functionName;

    /**
     * 合约传递参数
     */
    private String functionParam;

    /**
     * 执行结果哈希
     */
    private String hash;

    /**
     * 哈希状态
     */
    private String hashState;

    /**
     * 执行结果哈希
     */
    private String actionUid;
    /**
     * 执行结果哈希
     */
    private String contractWorkId;
    /**
     * 执行结果哈希
     */
    private String contractType;

}
