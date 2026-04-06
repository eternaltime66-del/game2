package org.wx.core.wxBase.context;

import lombok.Data;

import java.util.HashMap;

@Data
public class RequestContext {
    public HashMap<String,Object> data = new HashMap<>();
}
