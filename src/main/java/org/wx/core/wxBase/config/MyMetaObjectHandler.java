package org.wx.core.wxBase.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * MyBatis-Plus 字段自动填充处理器
 * 自动填充 createTime、updateTime、del 字段
 */
@Component // 必须加这个注解，否则Spring无法扫描到
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间（仅当字段值为null时填充）
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        // 填充更新时间
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
    }

    /**
     * 更新操作时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间（仅当字段值为null时填充）
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
        
        // 强制填充（可选）：
        // this.setFieldValByName("updateTime", new Date(), metaObject);
    }
}