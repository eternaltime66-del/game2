package org.wx.core.wxBusiness.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WxMoreLang 实体类
 * @author 无心
 * @date 2026-03-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_more_lang")
public class WxMoreLang extends WxBaseEntity<WxMoreLang> {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String msg;

    private String langCode;

    private String langMsg;

}
