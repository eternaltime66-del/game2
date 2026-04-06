package org.wx.core.wxBase.factory;


import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.unit.HttpRequestUnit;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.code.CodeEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 无心
 * @date 2021/7/23
 * @msg 备注
 * @demo SmsServiceImpl
 */
@Service("CodeFactory")
public class CodeFactory {
    /**
     * 开发者模式默认验证码码
     */
    public static final String DEF_CODE = "123456";

    public String sendCode(String phoneCode, String account, CodeEnum action) {
        String key = "code-send-" + action.toString() + "-" + phoneCode + account;
        String backCode = (String) Wx.RedisFactory.get(key);
        Long codeAcviteTime = 1L;
        //开发者模式时 验证码 默认为123456

        Long codeTime = 1L;
        Boolean sendRealCode = Wx.RedisFactory.get("EmailOrPhoneSendRealCode",Boolean.class);
        if (sendRealCode==null){
            sendRealCode = false;
        }
        String code = sendRealCode ? WordUnit.randomKey(6, 1) : DEF_CODE;
        ErrorFactory.throwError(backCode != null, "已发送验证码 "+codeTime+" 分钟内不可重复发送");
        //**********请与该区间段内编写第三方发送验证码业务*************
        if (sendRealCode) {
            sendEmail(account,code);
        }
        //***********************
        System.err.println(key + "---" + code);
        Wx.RedisFactory.setBuyMinute(key, code, codeAcviteTime);
        return code;
    }

    public static void main(String[] args) {
        sendEmail("23296066@qq.com","123456");
    }

    public static void sendEmail(String email,String code) {
        HttpRequestUnit requestUnit = new HttpRequestUnit();
        requestUnit.setContentType(HttpRequestUnit.ContentType.Json);
        requestUnit.setMethod(HttpRequestUnit.RequestMethod.POST);
        requestUnit.setUrl("https://www.aoksend.com/index/api/send_email");

        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "text");
        headers.put("Accept", "*/*");
        headers.put("User-Agent", "Apifox");
        headers.put("Connection", "keep-alive");

        requestUnit.setHeader(headers);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("app_key","32569099f638490ba42b024896ead196");
        jsonObject.put("template_id","E_139309850227");
        jsonObject.put("to",email);
        JSONObject param = new JSONObject();
        param.put("code",code);
        param.put("time",5);
        jsonObject.put("data",param.toJSONString());

        requestUnit.setSendJson(jsonObject);
        requestUnit.setJoinData(HttpRequestUnit.join(jsonObject));
        String string = requestUnit.request();
        System.out.println(string);
        requestUnit.logBody();
    }

    public void checkCode(String code, String account, CodeEnum action) {
//        if (true){
//            return;
//        }
        String key = "code-send-" + action.toString() + "-" + account;
        //System.err.println(key + "---" + code);
        String getCode = (String) Wx.RedisFactory.get(key);
        ErrorFactory.notNull(getCode, "未发送验证码 或 验证码已过期");
        ErrorFactory.throwError(!getCode.equals(code), "验证码错误");
    }

    public void delCode(String account, CodeEnum action) {
        String key = "code-send-" + action.toString() + "-" + account;
        Wx.RedisFactory.del(key);
    }

}
