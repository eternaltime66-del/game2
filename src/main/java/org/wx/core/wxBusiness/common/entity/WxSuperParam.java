package org.wx.core.wxBusiness.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;

/**
 * WxSuperParam 实体类
 * @author 无心
 * @date 2026-03-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_super_param")
public class WxSuperParam extends WxBaseEntity<WxSuperParam> {

    @TableId(type = IdType.AUTO)
    private String id;

    private String paramValue;

    private String paramType;
    private String paramName;

}
