package org.wx.core.wxBase.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 无心
 * @date 2023/4/20
 * @msg 备注
 * @demo RedisLock
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {
    /**
     * 要根据参数 名(key) 对应的值进行锁定
     */
    String key();
    /**
     * 是否针对方法绑定
     * funA("123")
     * funB("123")
     * true -  不同方法 相同的字符值 不会干扰 单方法锁定 A 和 B 都可以正常执行
     * false - 不同方法 相同的字符值 只会拿到锁的执行 同一时间 只有 A和B只有一个在执行
     */
    boolean bindMethod() default true;
    /**
     * 是否 等待
     * true     系统开始排队执行
     * false    直接打断没有拿到锁的方法运行
     */
    boolean loading() default false;
}
