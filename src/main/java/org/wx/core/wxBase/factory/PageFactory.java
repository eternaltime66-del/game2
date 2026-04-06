package org.wx.core.wxBase.factory;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.wx.core.wxBase.unit.HttpServletUnit;

import java.util.Optional;

/**
 * @author 无心
 * @date 2021/7/23
 * @msg mybatisPlus 分页工厂（并发安全版）
 */
public class PageFactory {
    private static final String PAGE_SIZE_PARAM_NAME = "size";
    private static final String PAGE_NO_PARAM_NAME = "current";
    // 默认分页值
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    // 最大页大小限制，防止内存溢出
    private static final int MAX_PAGE_SIZE = 100;

    private PageFactory() {
    }

    public static <T> Page<T> defaultPage() {
        int pageSize = DEFAULT_PAGE_SIZE;
        int pageNo = DEFAULT_PAGE_NUM;

        // 并发安全获取Request
        Optional<HttpServletRequest> requestOpt = HttpServletUnit.getRequest();
        if (requestOpt.isPresent()) {
            HttpServletRequest request = requestOpt.get();
            // 解析页大小（防数字格式异常+最大限制）
            pageSize = parsePageParam(request.getParameter(PAGE_SIZE_PARAM_NAME), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
            // 解析页码（防数字格式异常+最小限制）
            pageNo = parsePageParam(request.getParameter(PAGE_NO_PARAM_NAME), DEFAULT_PAGE_NUM, 1);
        } else {

        }

        return new Page<>((long) pageNo, (long) pageSize);
    }

    /**
     * 安全解析分页参数（防异常、限范围）
     */
    private static int parsePageParam(String paramVal, int defaultValue, int limit) {
        if (paramVal == null || paramVal.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int val = Integer.parseInt(paramVal.trim());
            // 页码：最小为1；页大小：最大为limit，最小为1
            return limit == 1 ? Math.max(val, limit) : Math.min(Math.max(val, 1), limit);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}