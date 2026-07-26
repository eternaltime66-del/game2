package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.common.entity.WxSuperParam;
import org.wx.core.wxBusiness.common.service.WxSuperParamService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-参数配置
 */
@RestController
@RequestMapping("/back/param")
public class B7SysParamController {

    @Resource
    public WxSuperParamService wxSuperParamService;

    /**
     * 参数配置列表（分页）
     */
    @PostMapping("/list")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<WxSuperParam>> list(
            @RequestBody WxSuperParam entity
    ) {
        entity.clearEmptyString();
        IPage<WxSuperParam> iPage = wxSuperParamService.pageQuery(entity);
        return WxResult.page(iPage);
    }

    /**
     * 添加或修改参数配置
     */
    @PostMapping("/update")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<?> addOrUpdate(
            @RequestBody WxSuperParam entity
    ) {
        entity.clearEmptyString();
        wxSuperParamService.saveOrUpdate(entity);
        return WxResult.success();
    }
}
