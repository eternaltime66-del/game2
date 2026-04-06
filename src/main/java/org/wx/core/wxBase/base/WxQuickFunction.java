package org.wx.core.wxBase.base;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import org.wx.core.wxBase.unit.JsonUnit;
import org.wx.core.wxBase.unit.ListUnit;
import org.wx.core.wxBusiness.account.entity.Member;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class WxQuickFunction {
    public static JsonUnit json(){
        return JsonUnit.init();
    }
    public static JsonUnit json(JSONObject jsonObject){
        return new JsonUnit(jsonObject);
    }

    public static Boolean isEmpty(Object object){
        return !notEmpty(object);
    }
    public static Boolean notEmpty(Object object){
        if (object==null){
            return false;
        }
        if (object.toString().isEmpty()){
            return false;
        }
        return true;
    }
    public static String futureBySecond(long seconds) {
        ZoneId zone = ZoneId.of("Asia/Shanghai"); // 东八区

        LocalDateTime time = LocalDateTime.now(zone)
                .plusSeconds(seconds);

        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String strArrAdd(String arr,String newStr){
        ArrayList<String> lits = ListUnit.toArr(arr);
        if (!lits.contains(newStr)){
            lits.add(newStr);
        }
        return String.join(",",lits);
    }

    public static String getEmail(String uid){
        Object obj = Wx.RedisFactory.get(quickUserKey(uid));
        if (obj!=null){
            JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(obj));
            return jsonObject.getString("email");
        }else {
            JSONObject jsonObject = loadUserInfo(uid);
            return jsonObject.getString("email");
        }
    }

    public static String geName(String uid){
        return "";
    }


    public static String getPic(String uid){
        return "";
    }

    private static JSONObject loadUserInfo(String uid){
        Member member = Wx.MemberService.getById(uid);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email",member.getEmail());
        jsonObject.put("name",member.getName());
        jsonObject.put("pic","");
        Wx.RedisFactory.setBuyHour(quickUserKey(uid),jsonObject.toJSONString(),1);
        return jsonObject;
    }

    private static String quickUserKey(String uid){
        return "QUICK_INFO_" + uid;
    }
}
