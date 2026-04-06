package org.wx.core.wxBusiness.code;

/**
 * 验证码枚举
 *
 * @author 29205
 */
public enum CodeEnum {
    //邮箱校验账户
    AccountCheckForEmail("邮箱校验账户");

    public final String msg;
    CodeEnum(String msg) {
        this.msg = msg;
    }
}
