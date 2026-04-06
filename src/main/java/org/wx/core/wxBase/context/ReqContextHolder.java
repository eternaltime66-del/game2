package org.wx.core.wxBase.context;

import lombok.val;

public class ReqContextHolder {

    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    public static void set(RequestContext ctx) {
        HOLDER.set(ctx);
    }

    public static RequestContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static void init(){
        set(new RequestContext());
    }

    public static void quickSet(String key,Object val){
        RequestContext ctx = get();
        if (ctx == null) {
            ctx = new RequestContext();
            set(ctx);
        }
        ctx.data.put(key, val);
    }

    public static String quickGet(String key){
        RequestContext ctx = get();
        if (ctx == null) {
            return "";
        }
        Object obj = ctx.data.get(key);
        if (obj==null){
            return "";
        }
        return obj.toString();

    }

}
