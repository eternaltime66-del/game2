package org.wx.core.wxBase.base;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 极简版动态查询DTO（子类继承专用，零泛型反射）
 * 核心能力：eq/like/in/min/max/start/end查询 + 多字段排序 + 逻辑删除过滤
 * 使用方式：子类继承并指定实体Class
 *
 * @author 无心
 * @date 2026/01/13
 */
@Slf4j
@Data
public class WxSuperDto<E extends WxBaseEntity<E>> {

    // 查询类型枚举（极简定义）
    private enum QueryType {
        EQ(""), LIKE("_like"), IN("_in"), MIN("_min"), MAX("_max"), GE("_start"), LE("_end");

        final String suffix;
        QueryType(String suffix) { this.suffix = suffix; }

        // 匹配查询类型（优先长后缀）
        static QueryType match(String key) {
            if (StringUtils.isBlank(key)) return EQ;
            return Arrays.stream(values())
                    .sorted((a, b) -> Integer.compare(b.suffix.length(), a.suffix.length()))
                    .filter(t -> !t.suffix.isEmpty() && key.endsWith(t.suffix))
                    .findFirst().orElse(EQ);
        }

        // 提取基础字段名
        static String getBaseField(String key) {
            QueryType type = match(key);
            return type == EQ ? key : key.substring(0, key.lastIndexOf(type.suffix));
        }
    }

    // 排序项（极简定义）
    @Data
    public static class SortItem {
        private String field;
        private String order = "asc";
    }

    // 基础参数
    @JsonIgnore
    @TableField(exist = false)
    private List<SortItem> sortList;

    // 核心缓存（子类初始化）
    @JsonIgnore
    @TableField(exist = false)
    private Map<String, String> fieldWhitelist; // 字段白名单：驼峰→下划线

    @TableField(exist = false)
    @JsonIgnore
    private final Map<String, Object> dynamicFields = new HashMap<>(16); // 动态字段容器

    // ========== 子类需覆写的核心方法（唯一需要手动的部分） ==========


    // ========== 核心修复：保证1级数据全量接收 ==========
    /**
     * 重写@JsonAnySetter：强制所有未绑定字段进入dynamicFields（包括1级字段）
     * 关键：移除过早的白名单校验，仅做空值过滤，校验延迟到构建Wrapper时
     */
    @JsonAnySetter
    public void setDynamicField(String key, Object value) {
        // 1. 空值过滤（宽松版：仅过滤null和纯空白字符串）
        if (value == null) return;
        Object actualValue = value;

        // 修复：字符串仅过滤纯空白，保留空字符串（""）和数字0等合法值
        if (value instanceof String) {
            String strVal = ((String) value).trim();
            // 仅过滤全空白的字符串，保留空字符串
            if (strVal.isEmpty() && ((String) value).isEmpty()) {
                actualValue = strVal; // 保留空字符串
            } else if (strVal.isEmpty()) {
                return; // 过滤纯空白（如"   "）
            } else {
                actualValue = strVal;
            }
        }

        // 2. 强制存储所有合法1级字段（核心修复：不再提前过滤）
        dynamicFields.put(key, actualValue);
        log.debug("接收1级字段：{} = {}", key, actualValue);
    }

    // 构建查询条件（核心方法）
    public QueryWrapper<E> buildQueryWrapper() {
        QueryWrapper<E> wrapper = new QueryWrapper<E>();

        initFieldWhitelist();

        // 解析动态查询条件
        dynamicFields.forEach((key, value) -> {
            String baseField = QueryType.getBaseField(key);
            String dbField = fieldWhitelist.get(baseField);
            if (dbField == null) return;

            String valStr = value.toString();
            switch (QueryType.match(key)) {
                case LIKE:
                    String safeVal = "%" + valStr.replace("%", "\\%").replace("_", "\\_") + "%";
                    wrapper.like(dbField, safeVal);
                    break;
                case IN:
                    List<String> inList = Arrays.stream(valStr.split(","))
                            .map(String::trim).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
                    if (!inList.isEmpty()) wrapper.in(dbField, inList);
                    break;
                case MIN: wrapper.ge(dbField, value); break;
                case MAX: wrapper.le(dbField, value); break;
                case GE: wrapper.ge(dbField, formatTime(valStr, " 00:00:00")); break;
                case LE: wrapper.le(dbField, formatTime(valStr, " 23:59:59")); break;
                default: wrapper.eq(dbField, value); break;
            }
        });

        // 构建排序
        if (Objects.nonNull(sortList) && !sortList.isEmpty()) {
            sortList.forEach(sort -> {
                if (sort == null || StringUtils.isBlank(sort.getField())) return;
                String field = sort.getField();
                if (!checkFieldWhitelist(field)) return;
                wrapper.orderBy(true, "asc".equalsIgnoreCase(sort.getOrder()), fieldWhitelist.get(field));
            });
        }

        return wrapper;
    }

    // ========== 工具方法（极简实现） ==========
    // 检查字段白名单（自动初始化）
    private boolean checkFieldWhitelist(String camelField) {
        if (StringUtils.isBlank(camelField)) return false;
        return fieldWhitelist.containsKey(camelField);
    }

    /**
     * 获取类的所有字段（含父类，过滤静态字段）
     * @param clazz 目标类
     * @return 所有非静态字段列表
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        // 递归终止条件：到 Object 类为止
        if (clazz == null || clazz == Object.class) {
            return fields;
        }

        // 1. 获取当前类声明的所有字段（含私有）
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            // 过滤静态字段（静态字段不属于实例字段，无需加入白名单）
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                fields.add(field);
                // 可选：设置私有字段可访问（如果需要读取/修改字段值）
                field.setAccessible(true);
            }
        }

        // 2. 递归获取父类字段（核心：向上遍历继承链）
        fields.addAll(getAllFields(clazz.getSuperclass()));
        return fields;
    }

    // 初始化字段白名单（子类实体）
    private void initFieldWhitelist() {
        if (fieldWhitelist != null) return;
        synchronized (this) {
            if (fieldWhitelist == null) {
                Map<String, String> whitelist = new HashMap<>(16);
             // 反射获取实体所有字段（含父类）
                List<Field> fields = getAllFields(this.getClass());
                // 构建白名单
                for (Field field : fields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                    String camel = field.getName();
                    String dbField = camel;

                    // 处理TableField注解
                    if (field.isAnnotationPresent(TableField.class)) {
                        TableField anno = field.getAnnotation(TableField.class);
                        if (!anno.exist()) continue;
                        if (StringUtils.isNotBlank(anno.value())) dbField = anno.value();
                    } else {
                        dbField = camelToUnderline(camel);
                    }
                    whitelist.put(camel, dbField);
                }
                System.out.println(JSONObject.toJSONString(whitelist));
                fieldWhitelist = Collections.unmodifiableMap(whitelist);
            }
        }
    }

    // 驼峰转下划线
    private String camelToUnderline(String str) {
        if (StringUtils.isBlank(str)) return str;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append("_");
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // 时间格式化
    private String formatTime(String val, String suffix) {
        if (StringUtils.isBlank(val) || val.contains(" ") || val.contains(":")) return val;
        if (val.matches("\\d{8}")) {
            try {
                val = String.format("%s-%s-%s", val.substring(0,4), val.substring(4,6), val.substring(6,8));
            } catch (Exception e) {
                log.warn("日期格式化失败:{}", val);
            }
        }
        return val + suffix;
    }


}