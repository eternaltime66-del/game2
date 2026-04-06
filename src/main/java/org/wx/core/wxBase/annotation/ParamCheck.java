package org.wx.core.wxBase.annotation;

//import childe.project.app.base.ErrorCode;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * controller 方法参数校验
 * @author 29205
 */
@Target(ElementType.PARAMETER)//parameter
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamCheck {

    //@ParamCheck(msg="类型",pattern="A|B",patternMsg="类型必须是A 或 B") String type
    //是否可空 默认不为空
    boolean notNull() default true;
    //参数描述 例: 密码
    String msg() default "";
    //正则
    String pattern() default "";
    //枚举过滤
    Class<?> enumPattern() default Object.class;
    //正则异常描述
    String patternMsg() default "";
    //数字小于零 默认不允许
    boolean lessZero() default false;

}
