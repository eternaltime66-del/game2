package org.wx.core.wxBase.base;



import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.wx.core.wxBase.context.ReqContextHolder;
import org.wx.core.wxBase.factory.CodeFactory;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.factory.RedisFactory;
import org.wx.core.wxBase.unit.HttpServletUnit;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.service.*;
import org.wx.core.wxBusiness.common.service.WxMoreLangService;
import org.wx.core.wxBusiness.common.service.WxSuperParamService;
import org.wx.core.wxBusiness.log.service.WxLogAsyncService;
import org.wx.core.wxBusiness.log.service.WxLogRequestDetailService;
import org.wx.core.wxBusiness.log.service.WxLogThirdPartyService;

/**
 * @author 无心
 * @date 2022/7/8
 * @msg 备注
 * @demo WX
 */
public final class Wx extends WxQuickFunction {

    private Wx() {}
    public static Boolean INIT = Boolean.FALSE;
    public static MemberService MemberService;
    public static RedisFactory RedisFactory;
    public static WxLogThirdPartyService WxLogThirdPartyService;
    public static WxLogRequestDetailService WxLogRequestDetailService;
    public static WxLogAsyncService LogAsyncService;
    public static Web3WalletService Web3WalletService;
    public static PointWalletService PointWalletService;
    public static Web3CoinService Web3CoinService;
    public static MoneyRecordService MoneyRecordService;
    public static Web3RunWatchService Web3RunWatchService;
    public static WxMoreLangService WxMoreLangService;
    public static WxSuperParamService SuperParamService;
    public static Web3RechargeService Web3RechargeService;
    public static CodeFactory CodeFactory = new CodeFactory();
    static void init(WxSuperServices services) {
        MemberService = services.getMemberService();
        RedisFactory = services.getRedisFactory();
        WxLogThirdPartyService = services.getWxLogThirdPartyService();
        WxLogRequestDetailService = services.getWxLogRequestDetailService();
        LogAsyncService = services.getWxLogAsyncService();
        Web3WalletService = services.getWeb3WalletService();
        PointWalletService = services.getPointWalletService();
        Web3CoinService = services.getWeb3CoinService();
        MoneyRecordService = services.getMoneyRecordService();
        Web3RunWatchService = services.getWeb3RunWatchService();
        WxMoreLangService = services.getWxMoreLangService();
        SuperParamService = services.getWxSuperParamService();
        Web3RechargeService = services.getWeb3RechargeService();
        System.out.println("初始化完毕");
        INIT = Boolean.TRUE;
    }

    public static String token(){
        HttpServletRequest request = HttpServletUnit.request();
        return request.getHeader("token");
    }
    public static String lang(){
        HttpServletRequest request = HttpServletUnit.request();
        return request.getHeader("lang");
    }

    public static Member member(){
        String token = token();

        ErrorFactory.throwError(Wx.isEmpty(token),"403","登录超时");
        Object tokenVal = Wx.RedisFactory.get(token);
        ErrorFactory.throwError(Wx.isEmpty(tokenVal),"403","登录超时");
        String uid = tokenVal.toString();

        Member member = MemberService.getById(uid);
        ErrorFactory.throwError(member==null,"403","登录超时");
        ReqContextHolder.quickSet("uid",member.getId());
        return member;
    }

    public static void main(String[] args) {
        System.out.println(Boolean.getBoolean("1"));
    }
    public static String memberId(){
        return member().getId();
    }

    public static String TO_ADDRESS = "0x871c23A0CD5Fe8E1bb0fEcaf63822B32EC2e0FF5";
//    public static String ADDRESS_ADDRESS = "0xb397b1523357de37Df31A5e90aa5e08115545A96";
//    public static String ADDRESS_PRV = "f78b2737e4357851c567d8b6cdb106c28c0a35c17bb6c24f0e2973bcd7270360";

    public static JSONObject budLog(JSONObject json,String info,Object... param){
        int size = json.size();
        json.put(size+1+"",String.format(info,param));
        return json;
    }
}
