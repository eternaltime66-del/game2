package org.wx.core.wxBusiness.common.service;

import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.common.entity.WxMoreLang;
import org.wx.core.wxBusiness.common.mapper.WxMoreLangMapper;
import org.springframework.stereotype.Service;

/**
 * WxMoreLang Service实现类
 * @author 无心
 * @date 2026-03-08
 */
@Service
public class WxMoreLangService extends WxServiceImpl<WxMoreLangMapper, WxMoreLang> {

    @Transactional(rollbackFor = Exception.class)
    public String getLangMsg(String msg,String langCode) {
        WxMoreLang lang = new WxMoreLang();
        lang.setMsg(msg);
        lang.setLangCode(langCode);
        WxMoreLang one = this.find().entity(lang).one();
        if (one!=null){
            return one.getLangMsg();
        }else {
            lang.setLangMsg(msg+"待翻译"+langCode+"语言");
            this.save(lang);
            return lang.getLangMsg();
        }
    }
}
