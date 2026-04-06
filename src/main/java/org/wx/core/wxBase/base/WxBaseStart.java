package org.wx.core.wxBase.base;



import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * @author 无心
 * @msg 项目启动自动执行
 */
@Component
@Order(value = 1)
public class WxBaseStart implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.err.println("项目已运行");
        System.err.println("自执行方法开启");
        System.err.println("执行完毕");
    }


}
