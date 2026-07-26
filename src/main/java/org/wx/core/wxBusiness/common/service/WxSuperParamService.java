package org.wx.core.wxBusiness.common.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.convert.ConvertException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.common.entity.WxSuperParam;
import org.wx.core.wxBusiness.common.mapper.WxSuperParamMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * WxSuperParam Service实现类
 * @author 无心
 * @date 2026-03-12
 */
@Service
public class WxSuperParamService extends WxServiceImpl<WxSuperParamMapper, WxSuperParam> {


    
    public <T> T getVal(String key, Class<T> clazz) {
        WxSuperParam param = this.getById(key);
        try {
            //System.err.println(key);
            return Convert.convert(clazz, param.getParamValue());
        } catch (ConvertException e) {
            //System.err.println(key+"---不存在");
            e.printStackTrace();
        }
        return null;
    }

    
    public Boolean getBoolean(String key){
        WxSuperParam param = this.getById(key);
        if (param==null){
            return false;
        }
        return param.getParamValue().equals("1");
    }

    public String getVal(String key){
        return this.getVal(key,String.class);
    };


    public String getString(String key, String def){
        WxSuperParam param = this.getById(key);
        if (param==null){
            return def;
        }
        return this.getVal(key,String.class);
    };

    public Integer getInteger(String key, Integer def){
        WxSuperParam param = getById(key);
        if (param==null){
            return def;
        }
        return this.getVal(key,Integer.class);
    };



    public void setVal(String key,Object val){
        this.update(
                new LambdaUpdateWrapper<WxSuperParam>()
                        .eq(WxSuperParam::getId,key)
                        .set(WxSuperParam::getParamValue,val)
        );
    }


    
    public BigDecimal getBigDecimal(String key){
        return this.getVal(key,BigDecimal.class);
    }

    
    public BigDecimal getBigDecimal(String key, String def){
        WxSuperParam param = this.getById(key);
        if (param==null){
            WxSuperParam wxSuperParam = new WxSuperParam();
            wxSuperParam.setId(key);
            wxSuperParam.setParamValue(def);
            this.save(wxSuperParam);
            return new BigDecimal(def);
        }
        return this.getVal(key,BigDecimal.class);
    }

    
    public BigDecimal getRate(String key){
        return this.getVal(key,BigDecimal.class).divide(BigDecimal.valueOf(100),4, RoundingMode.FLOOR);
    }
    

}
