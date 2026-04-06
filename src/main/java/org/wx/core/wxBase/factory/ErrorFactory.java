package org.wx.core.wxBase.factory;


import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.exception.WxApiException;

/**
 * @author 无心
 * @date 2021/7/23
 * @msg mybatisPlus 分页工厂
 */
public class ErrorFactory {
    public static void throwError(Boolean flog,String code,String msg){
//        if (Wx.WxMoreLangService!=null){
//            msg = Wx.WxMoreLangService.getLangMsg(msg,Wx.lang());
//        }
        if (flog){
            throw new WxApiException(code,msg);
        }
    }
    public static void throwError(Boolean flog,String msg){
        throwError(flog,"500",msg);
    }
    public static void redisLockError(){
        throwError(true ,"6379","请求过于频繁 请稍后再试");
    }

    public static void redisLockError(String msg){
        throwError(true ,"6379",msg);
    }


    public static void throwError(String msg){
        throwError(true,msg);
    }


    public static void notNull(Object flog,String msg){
        throwError(flog==null,msg);
    }


    public static void notEmpty(Object flog,String msg){
        notNull(flog,msg);
        throwError(flog.toString().isEmpty(),msg);
    }



    public static void notEquals(String str1,String str2,String msg){
        throwError(!str1.equals(str2),msg);
    }


}
