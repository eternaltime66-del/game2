package org.wx.core.wxBase.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

/**
 * 基础实体类（通用审计+逻辑删除）
 * 所有业务实体需继承此类，统一管理通用字段
 *
 * @author 无心
 * @date 2022/4/27
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WxBaseEntity<E extends WxBaseEntity<E>> extends WxSuperDto<E>{

    /**
     * 创建时间
     */
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间（
     */
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @SneakyThrows
    public void clearEmptyString(){
        Object obj = this;
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == String.class) {
                field.setAccessible(true); // 绕过私有字段的限制
                String value = (String) field.get(obj);
                if ("".equals(value)) {
                    field.set(obj, null);
                }
            }
        }
    }

    public void vo(){

    }


}