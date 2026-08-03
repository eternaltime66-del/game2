package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.api.vo.CommonIdVo;
import org.wx.core.wxBusiness.game.entity.AdminRecipeVo;
import org.wx.core.wxBusiness.game.entity.GameRecipe;
import org.wx.core.wxBusiness.game.service.GameRecipeAdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/** 后台-合成配方 */
@RestController
@RequestMapping("/back/game/recipe")
public class B16GameRecipeController {

    @Resource
    private GameRecipeAdminService adminService;

    @PostMapping("/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<AdminRecipeVo>> list(@RequestBody GameRecipe entity) {
        entity.clearEmptyString();
        IPage<AdminRecipeVo> page = adminService.listRecipes(entity);
        return WxResult.page(page);
    }

    @PostMapping("/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminRecipeVo> detail(@ParamCheck(msg = "配方ID") String id) {
        return WxResult.success(adminService.getRecipeDetail(id));
    }

    @PostMapping("/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminRecipeVo> save(@RequestBody AdminRecipeVo vo) {
        return WxResult.success(adminService.saveRecipe(vo));
    }

    @PostMapping("/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> remove(@RequestBody CommonIdVo vo) {
        adminService.removeRecipe(vo.stringId());
        return WxResult.success();
    }
}
