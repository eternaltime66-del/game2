package org.wx.core.wxBusiness.api.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBusiness.account.entity.MoneyRecord;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.account.service.MoneyRecordService;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.util.List;

/**
 * 后台-流水
 */
@RestController
@RequestMapping("/back/record")
public class B8MoneyRecordController {

    @Resource
    public MoneyRecordService moneyRecordService;

    /**
     * 资金流水列表（分页）
     */
    @PostMapping("/list")
    @WxRequestLog()
    @NeedHeader(roles = {MemberRole.ADMIN})
    public WxResult<List<MoneyRecord>> list(
            @RequestBody MoneyRecord entity
    ) {
        entity.clearEmptyString();
        IPage<MoneyRecord> iPage = moneyRecordService.pageQuery(entity);
        return WxResult.page(iPage);
    }



}
