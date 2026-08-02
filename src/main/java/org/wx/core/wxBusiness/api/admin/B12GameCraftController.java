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
import org.wx.core.wxBusiness.game.entity.AdminCraftRecipeVo;
import org.wx.core.wxBusiness.game.entity.GameCraftRecipe;
import org.wx.core.wxBusiness.game.entity.ItemTagOptionVo;
import org.wx.core.wxBusiness.game.service.GameCraftAdminService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-合成配方
 */
@RestController
@RequestMapping("/back/game/craft")
public class B12GameCraftController {

    @Resource
    private GameCraftAdminService craftAdminService;

    @PostMapping("/recipe/list")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<GameCraftRecipe>> recipeList(@RequestBody GameCraftRecipe entity) {
        entity.clearEmptyString();
        IPage<GameCraftRecipe> page = craftAdminService.listRecipes(entity);
        return WxResult.page(page);
    }

    @PostMapping("/recipe/detail")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminCraftRecipeVo> recipeDetail(@ParamCheck(msg = "配方ID") String recipeId) {
        return WxResult.success(craftAdminService.getDetail(recipeId));
    }

    @PostMapping("/recipe/save")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<AdminCraftRecipeVo> recipeSave(@RequestBody AdminCraftRecipeVo entity) {
        return WxResult.success(craftAdminService.saveRecipe(entity));
    }

    @PostMapping("/recipe/remove")
    @WxRequestLog()
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<?> recipeRemove(@RequestBody CommonIdVo vo) {
        craftAdminService.deleteRecipe(vo.stringId());
        return WxResult.success();
    }

    @PostMapping("/item-tag/options")
    @WxRequestLog(recordRequest = false, recordResponse = false)
    @NeedHeader(roles = MemberRole.ADMIN)
    public WxResult<List<ItemTagOptionVo>> itemTagOptions() {
        return WxResult.success(craftAdminService.listItemTagOptions());
    }
}
