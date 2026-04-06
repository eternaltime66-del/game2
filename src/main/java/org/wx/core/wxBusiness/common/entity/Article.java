package org.wx.core.wxBusiness.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Article 实体类
 * @author 无心
 * @date 2026-02-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_article")
public class Article extends WxBaseEntity<Article> {

    /**
     * 文章表
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

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

    /**
     * 视频链接
     */
    private String videoUrl;

    /**
     * 面向用户
     */
    private String uid;

    /**
     * 已读用户
     */
    private String readAlreadyUid;

}
