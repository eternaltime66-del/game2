package org.wx.core.wxBase.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBase.exception.WxApiException;

@Slf4j
@RestControllerAdvice
@Order(0)
public class GlobalExceptionHandler {

    /**
     * ✅ 业务异常（你原来的）
     */
    @ExceptionHandler(WxApiException.class)
    public WxResult<Object> apiException(WxApiException ex) {
        WxResult<Object> result = new WxResult<>();
        result.setCode(ex.getCode());
        result.setMsg(ex.getMsg());
//        result.setCnMsg(ex.getCnMsg());
        result.setSuccess(false);
        return result;
    }

    /**
     * ✅ 参数校验异常（@Valid / @Validated）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public WxResult<Object> validException(Exception ex) {
        String msg = "参数错误";
        if (ex instanceof MethodArgumentNotValidException e) {
            msg = e.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .findFirst()
                    .map(err -> err.getDefaultMessage())
                    .orElse(msg);
        }
        return WxResult.error("400", msg);
    }

    /**
     * ✅ 运行时异常（兜底）
     */
    @ExceptionHandler(RuntimeException.class)
    public WxResult<Object> runtimeException(RuntimeException ex) {
        log.error("运行时异常", ex);
        return WxResult.error("500", ex.getMessage());
    }

//    /**
//     * ✅ 所有异常兜底（防止漏网）
//     */
//    @ExceptionHandler(Exception.class)
//    public WxResult<Object> exception(Exception ex) {
//        log.error("系统异常", ex);
//        return WxResult.error("500", "系统繁忙，请稍后再试");
//    }
}
