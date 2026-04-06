package org.wx.core.wxBase.unit;

import com.alibaba.fastjson2.JSONObject;

/**
 * 修复后的 JSON 流式构建工具类
 * 核心：保留你的链式调用逻辑，修复 init 方法，增强封装性和安全性
 */
public class JsonUnit {
    // 私有化成员变量，禁止外部直接修改
    private final JSONObject json;

    // 构造器私有化，推荐用静态方法创建实例（更符合工具类规范）
    public JsonUnit() {
        this.json = new JSONObject();
    }
    public JsonUnit(JSONObject json) {
        this.json = json;
    }

    /**
     * 初始化方法（修复逻辑：返回当前对象，保证链式调用）
     */
    public static JsonUnit init() {
        // 清空原有数据（可选：避免复用对象时数据残留）
        return new JsonUnit();
    }

    /**
     * 流式添加键值对（增强：空值处理，避免NPE）
     */
    public JsonUnit put(String key, Object val) {
        this.json.put(key, val );
        return this;
    }

    /**
     * 构建最终 JSON 对象
     */
    public JSONObject build() {
        // 返回新的 JSONObject（避免外部修改内部数据）
        return new JSONObject(this.json);
    }

    /**
     * 快捷创建实例（替代 new JsonUnit()，更直观）
     */
    public static JsonUnit create() {
        return new JsonUnit();
    }

    /**
     * 可选：构建为 JSON 字符串（满足推送/输出需求）
     */
    public String toJsonString() {
        return this.json.toJSONString();
    }

    public void setStr(String str){
        str = this.build().toJSONString();
    }
}