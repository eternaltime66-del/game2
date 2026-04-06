package org.wx.core.wxBase.base;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class WxResult<T> {

    /**
     * 状态码
     */
    private String code = "200";
    /**
     * 相关信息 * 带多语言
     */
    private String msg = "successful purchase";

    /**
     * 详细数据
     */
    private T data;


    /**
     * 注册&登录时返回 用户token
     */
    private String token;

    /**
     * 请求状态 成功/失败
     */
    private Boolean success = true;

    /**
     * 相关信息 中文版
     */
    private String cnMsg;

    /**
     * 分页组件
     */
    private Pager pager;

    @Data
    static class Pager {
        /**
         * 页数
         */
        private Integer pageNo = 1;
        /**
         * 美页条数
         */
        private Integer pageSize = 20;
        /**
         * 总页数
         */
        private Integer totalPage = 0;
        /**
         * 总条数
         */
        private Integer totalRows = 0;

    }

    public WxResult(){

    }

    public static <T>WxResult<T> success(){
        return new WxResult<>();
    }
    public static <T>WxResult<T> error(String code,String msg){
        WxResult<T> result = new WxResult<T>();
        result.setCode(code);
        result.setMsg(msg);
        result.setSuccess(false);
        return result;
    }

    public static <T>WxResult<T> token(String token){
        WxResult<T> objectWxResult = new WxResult<>();
        objectWxResult.setToken(token);
        return objectWxResult;
    }

    public static <T>WxResult<T> success(T data){
        WxResult<T> tWxResult = new WxResult<T>();
        tWxResult.setData(data);
        return tWxResult;
    }

    public static <T>WxResult<List<T>> page(IPage<T> page){
        WxResult<List<T>> tWxResult = new WxResult<List<T>>();
        tWxResult.data = page.getRecords();
        tWxResult.code = "200";
        tWxResult.success = true;
        tWxResult.msg = "successful purchase";
        tWxResult.pager = new Pager();
        tWxResult.pager.setPageNo(Convert.toInt(page.getCurrent()));
        tWxResult.pager.setTotalRows(Convert.toInt(page.getTotal()));
        tWxResult.pager.setPageSize(Convert.toInt(page.getSize()));
        tWxResult.pager.setTotalPage(PageUtil.totalPage(tWxResult.pager.getTotalRows(), tWxResult.pager.getPageSize()));
        return tWxResult;
    }
}
