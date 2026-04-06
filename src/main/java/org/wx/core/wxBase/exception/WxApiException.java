package org.wx.core.wxBase.exception;

import lombok.Data;

/**
 * @author 无心
 * @date 2023/5/11
 * @msg 备注
 * @demo WxApiException
 */
@Data
public class WxApiException extends RuntimeException{
    String code;
    String msg;
    String cnMsg;

    public WxApiException() {

    }

    public WxApiException(String code,String cnMsg) {
        super(cnMsg);
        this.code = code;
        this.msg = cnMsg;
        this.cnMsg = cnMsg;
    }

    public WxApiException(String code,String langMsg,String cnMsg) {
        super(cnMsg);
        this.code = code;
        this.msg = langMsg;
        this.cnMsg = cnMsg;
    }
}
