package org.wx.core.wxBusiness.api.admin;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.MemberKyc;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.account.service.MemberService;
import org.wx.core.wxBusiness.api.vo.CommonIdVo;
import org.wx.core.wxBusiness.common.entity.Article;
import org.wx.core.wxBusiness.common.service.ArticleService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-内容管理
 */
@RestController
@RequestMapping("/back/media")
public class B4MediaController {

    @Resource
    public MemberService memberService;

    @Resource
    public ArticleService articleService;

    /**
     * 文章列表
     */
    @PostMapping("/article/list")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<Article>> articleList(
            @RequestBody Article entity
    ) {
        return WxResult.page(articleService.pageQuery(entity));
    }

    /**
     * 删除文章
     */
    @PostMapping("/article/remove/byId")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<MemberKyc>> articleRemoveById(
            @RequestBody CommonIdVo vo
    ) {
        articleService.removeById(vo.getId());
        return WxResult.success();
    }

    /**
     * 添加或修改文章(传id修改 不传添加)
     */
    @PostMapping("/article/saveOrUpdate")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<Object> saveOrUpdate(
            @RequestBody Article entity
    ) {
        articleService.saveOrUpdate(entity);
        return WxResult.success();
    }

}
