package org.wx.core.wxBusiness.api.vo;

import lombok.Data;
import org.wx.core.wxBase.factory.ErrorFactory;

import java.io.Serializable;

@Data
public class CommonIdVo {
    /**
     * id
     */
    Serializable id;

    public String stringId(){
        if (this.id==null){
            ErrorFactory.throwError("Id 必传");
        }
        return String.valueOf(id);
    }
}
