package org.wx.core.wxBusiness.api.common;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class CommonController {

    @RequestMapping("/")
    public String fun(
    ) {
        return "404";
    }


}
