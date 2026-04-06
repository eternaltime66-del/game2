package org.wx.core;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBusiness.account.service.MemberService;

@SpringBootTest
class Web3ApplicationTests {

    @Resource
    public MemberService memberService;
    @Test
    void contextLoads() {
        Wx.RedisFactory.setBuySeconds("x1","ox2",200);
        System.out.println(Wx.RedisFactory.get("x1"));
    }

}
