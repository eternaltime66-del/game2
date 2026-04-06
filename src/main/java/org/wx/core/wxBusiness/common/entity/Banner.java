package org.wx.core.wxBusiness.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Banner 实体类
 * @author 无心
 * @date 2026-03-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_banner")
public class Banner extends WxBaseEntity<Banner> {

    /**
     * 文章表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 图片链接
     */
    private String imageUrl;

    /**
     * 跳转链接
     */
    private String jumpUrl;

    /**
     * 多语言
     */
    private String lang;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 类型
     */
    private String type;

}
