package org.wx.core.wxBusiness.media.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.wx.core.wxBase.base.WxBaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HzCrowdFundingContent 实体类
 * @author 无心
 * @date 2026-01-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hz_crowd_funding_content")
public class HzCrowdFundingContent extends WxBaseEntity<HzCrowdFundingContent> {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 内容名称
     */
    private String contentName;

    /**
     * Logo来源: 0=from_library, 1=upload
     */
    private Object logoSourceType;

    /**
     * 若从库中选择，则为logo资源ID
     */
    private String logoId;

    /**
     * Logo图片URL路径
     */
    private String logoUrl;

    /**
     * Logo在OSS中的唯一标识（object key）
     */
    private String logoOssId;

    /**
     * 媒体类型: 1=video,2=voice,3=music
     */
    private String resourceType;

    /**
     * 资源来源: 0=from_library, 1=upload
     */
    private Object resourceSourceType;

    /**
     * 资源资源ID（从库中选择）
     */
    private String resourceId;

    /**
     * 上传资源的URL路径
     */
    private String resourceUrl;

    /**
     * 资源在OSS中的唯一标识（object key）
     */
    private String resourceOssId;

    /**
     * 状态: 1=待发起, 10=已发起
     */
    private Integer status;

    /**
     * 是否可篡改: 1=是, 0=否
     */
    private Object isEditable;

    /**
     * 是否收藏: 1=是, 0=否
     */
    private Object isFavorited;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 创建部门
     */
    private Integer createDept;



}
