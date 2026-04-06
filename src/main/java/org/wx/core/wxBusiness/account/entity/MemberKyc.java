package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBusiness.account.entity.enums.MemberKycState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MemberKyc 实体类
 * @author 无心
 * @date 2026-02-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_member_kyc")
public class MemberKyc extends WxBaseEntity<MemberKyc> {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String uid;

    /**
     * 护照号码
     */
    private String passPort;

    /**
     * 用户名
     */
    private String userName;


    /**
     * 用户名
     */
    private String email;

    /**
     * 状态
     */
    private MemberKycState state;

}
