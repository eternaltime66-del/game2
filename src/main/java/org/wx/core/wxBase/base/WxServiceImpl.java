package org.wx.core.wxBase.base;


import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.factory.PageFactory;
import org.wx.core.wxBase.unit.HttpServletUnit;
import org.wx.core.wxBusiness.log.entity.WxLogRequestDetail;
import org.wx.core.wxBusiness.log.service.WxLogRequestDetailService;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * 通用Service实现类（优化版：抽离safeWrapper方法）
 *
 * @param <M> Mapper类型
 * @param <T> 实体类型
 * @author 无心
 * @date 2026/01/13
 */
@Getter
@Slf4j
public abstract class WxServiceImpl<M extends BaseMapper<T>, T extends WxBaseEntity<T>> extends ServiceImpl<M, T> {



    // ===================== 分页查询（复用safeWrapper） =====================

    public IPage<T> pageQuery(LambdaQueryWrapper<T> wrapper) {
        Page<T> page = PageFactory.defaultPage();
        IPage<T> resultPage = this.baseMapper.selectPage(page, wrapper);

        if (resultPage.getRecords() != null && !resultPage.getRecords().isEmpty()) {
            processEntityList(resultPage.getRecords());
        }
        return super.page(page, wrapper);
    }
    /**
     * 分页查询（核心方法）
     *
     * @return 分页结果
     */
    public IPage<T> pageQuery(T entity) {

        if (entity == null) {
            throw new IllegalArgumentException("查询实体不能为空！");
        }
        entity.clearEmptyString();
        Page<T> page = PageFactory.defaultPage();

        // 3. 构建查询条件
        QueryWrapper<T> wrapper = entity.buildQueryWrapper();
        wrapper.orderByDesc("create_time");
        wrapper.setEntity(entity);
        IPage<T> resultPage = this.page(page, wrapper);

        if (resultPage.getRecords() != null && !resultPage.getRecords().isEmpty()) {
            processEntityList(resultPage.getRecords());
        }

        return resultPage;
    }

    // ===================== 列表查询（复用safeWrapper） =====================
    public List<T> listQuery(T entity) {
        entity.clearEmptyString();
        Wrapper<T> wrapper = entity.buildQueryWrapper();
        ;
        List<T> list = this.list(wrapper);
        processEntityList(list);
        return list;
    }

    // ===================== 单条查询（复用safeWrapper） =====================
    public T getOneQuery(T entity) {
        List<T> list = listQuery(entity);
        if (list.isEmpty()) {
            return null;
        }
        ErrorFactory.throwError(list.size() > 1, "查询结果超过1条，请检查查询条件");
        return list.get(0);
    }

    // ===================== 原有方法保留 =====================


    protected void processEntityList(List<T> list) {
        list.forEach(T::vo);
    }
    public String sumQuery(T entity, String field) {
        entity.clearEmptyString();
        return sumQuery(entity.buildQueryWrapper(), field, field);
    }

    public String sumQuery(QueryWrapper<T> wrapper, String field, String alias) {
        String sumSql = "(IFNULL(sum(cast(`" + field + "` as decimal(18, 8))),0)) as " + alias;
        wrapper.select(sumSql);
        List<BigDecimal> result = this.baseMapper.selectObjs(wrapper);
        return result.isEmpty() ? "0" : result.get(0).stripTrailingZeros().toPlainString();
    }


    @Override
    public boolean save(T entity) {
        return super.save(entity);
    }

    @Override
    public boolean updateById(T entity) {
        return wxUpdateById(entity);
    }

    /**
     * 根据实体类的 id&&eq 进行 修改
     */

    @SafeVarargs
    public final boolean wxUpdateById(T e, SFunction<T, Object>... funs) {
        // ========== 1. 前置参数校验 ==========
        if (e == null) {
            throw new IllegalArgumentException("更新实体不能为空");
        }

        // ========== 2. 解析实体ID和基础信息 ==========
        JSONObject entityJson = JSONObject.parseObject(JSONObject.toJSONString(e));
        String id = entityJson.getString("id");
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("实体ID不能为空");
        }
        // 获取实体对应的表名（基于MyBatis-Plus注解，也可自定义逻辑）
        String tableName = getTableName(e.getClass());

        // ========== 3. 获取请求上下文 ==========
        HttpServletRequest request = HttpServletUnit.request();
        boolean needLog = request != null && Boolean.TRUE.equals(request.getAttribute("ReqLogChange"));
        needLog = needLog && !(e.getClass()==WxLogRequestDetail.class);
        String reqLogId = request != null ? (String) request.getAttribute("ReqLogId") : UUID.randomUUID().toString().replace("-", "");
        // 默认操作人ID（可从请求上下文/登录态获取，这里先给默认值）
        String operatorId = request != null ? (String) request.getAttribute("ReqUserId") : "";

        // ========== 4. 查询更新前数据（仅记录变动字段） ==========
        JSONObject beforeJson = new JSONObject();
        if (needLog) {
            T beforeEntity = this.getOne(new QueryWrapper<T>().eq("id", id));
            if (beforeEntity != null) {
                beforeJson = JSONObject.parseObject(JSONObject.toJSONString(beforeEntity));
            }
        }

        // ========== 5. 构建更新条件器 ==========
        UpdateWrapper<T> wrapperx = new UpdateWrapper<>();
        wrapperx.eq("id", id);
        LambdaUpdateWrapper<T> wrapper = wrapperx.lambda();
        boolean result = false;
        if (funs == null || funs.length == 0) {
            result= super.updateById(e);
        }else {
            for (SFunction<T, Object> fun : funs) {
                wrapper.set(fun, fun.apply(e));
            }
            result= this.update(wrapper);
        }

        // ========== 7. 查询更新后数据（仅记录变动字段） ==========
        JSONObject afterJson = new JSONObject();
        Map<String, Object> afterData = new HashMap<>();
        if (needLog) {
            T afterEntity = this.getOne(new QueryWrapper<T>().eq("id", id));
            if (afterEntity != null) {
                afterJson = JSONObject.parseObject(JSONObject.toJSONString(afterEntity));
            }
        }

        // ========== 8. 记录变更日志 ==========
        if (needLog) {
            WxLogRequestDetail logEntity = new WxLogRequestDetail();
            logEntity.setId(UUID.randomUUID().toString().replace("-", "")); // 日志主键
            logEntity.setRequestId(reqLogId); // 关联请求ID
            logEntity.setTableName(tableName); // 操作表名
            logEntity.setActionType("UPDATE"); // 操作类型：更新
            Map<String, String> change = getChangedFieldsJson(beforeJson, afterJson);
            logEntity.setBeforeData(change.get("before")); // 变更前（仅ID+变动字段）
            logEntity.setAfterData(change.get("after")); // 变更后（仅ID+变动字段）
            logEntity.setChangeData(change.get("change"));
            logEntity.setOperatorId(operatorId); // 操作人ID
            // 保存日志（使用静态调用方式，适配你的项目规范）
            try {
                Wx.WxLogRequestDetailService.save(logEntity);
                log.info("更新日志记录成功，请求ID：{}，表名：{}，ID：{}", reqLogId, tableName, id);
            } catch (Exception ex) {
                log.error("保存更新日志失败，请求ID：{}", reqLogId, ex);
            }
        }
        return result;
    }

    /**
     * 对比两个JSON，返回「ID+变动字段」的JSON字符串
     * @param beforeJson 变更前JSON
     * @param afterJson 变更后JSON
     * @return 仅包含ID和变动字段的JSON（before/after分别返回）
     */
    public static Map<String, String> getChangedFieldsJson(JSONObject beforeJson, JSONObject afterJson) {
        Map<String, String> resultMap = new HashMap<>(3);
        JSONObject beforeChanged = new JSONObject();
        JSONObject afterChanged = new JSONObject();
        JSONObject changeDesc =new JSONObject();; // 多字段变动用分号分隔

        // 1. 先获取ID（必须保留）
        String idKey = "id";
        Object id = beforeJson.get(idKey);
        if (id == null) {
            id = afterJson.get(idKey);
        }
        if (id != null) {
            beforeChanged.put(idKey, id);
            afterChanged.put(idKey, id);
            changeDesc.put(idKey , id); // ID加入变动描述
        }

        // 2. 遍历所有字段，筛选出值不同的字段
        if (afterJson != null) {
            for (String key : afterJson.keySet()) {
                // 跳过ID（已单独处理）
                if (idKey.equals(key)) {
                    continue;
                }
                // 获取前后值
                Object beforeVal = beforeJson.get(key);
                Object afterVal = afterJson.get(key);

                // 对比值是否不同（处理null的情况）
                boolean isChanged = false;
                if (beforeVal == null && afterVal != null) {
                    isChanged = true;
                } else if (beforeVal != null && !beforeVal.equals(afterVal)) {
                    isChanged = true;
                }

                // 仅保留变动的字段
                if (isChanged) {
                    // 填充before/after JSON
                    beforeChanged.put(key, beforeVal);
                    afterChanged.put(key, afterVal);

                    // 生成该字段的变动描述（处理null值显示）
                    String beforeStr = beforeVal == null ? "null" : beforeVal.toString();
                    String afterStr = afterVal == null ? "null" : afterVal.toString();
                    changeDesc.put(key, beforeStr + " >>> " + afterStr);
                }
            }
        }

        // 3. 封装结果
        resultMap.put("before", beforeChanged.toJSONString());
        resultMap.put("after", afterChanged.toJSONString());
        resultMap.put("change", changeDesc.toString()); // 新增变动描述
        return resultMap;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 获取实体对应的数据库表名（适配MyBatis-Plus的@TableName注解）
     */
    private String getTableName(Class<?> entityClass) {
        com.baomidou.mybatisplus.annotation.TableName tableNameAnnotation = entityClass.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
        if (tableNameAnnotation != null && StringUtils.hasText(tableNameAnnotation.value())) {
            return tableNameAnnotation.value();
        }
        // 无注解时，默认使用类名转下划线（如SysUser → sys_user）
        return com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(entityClass.getSimpleName());
    }




    @Override
    public T getById(Serializable id) {
        T entity = super.getById(id);
        if (entity != null) {
            processEntityList(List.of(entity));
        }
        return entity;
    }

    // ===================== Lambda 链式查询核心（支持指定查询字段） =====================

    /**
     * Lambda 链式查询入口（类型安全，支持指定查询字段）
     */
    public WxLambdaChainQuery find() {
        return new WxLambdaChainQuery();
    }

    public WxLambdaChainQuery find(LambdaQueryWrapper<T> wrapper) {
        WxLambdaChainQuery chainQuery = new WxLambdaChainQuery();
        chainQuery.setWrapper(wrapper);
        return chainQuery;
    }

    /**
     * Lambda 链式查询器（支持指定查询字段）
     */
    @Data
    public class WxLambdaChainQuery {
        private  LambdaQueryWrapper<T> wrapper = new QueryWrapper<T>().lambda();

        private IPage<T> page;
        // 标记是否手动指定了查询字段
        private Boolean hasSelectFields = false;

        public LambdaQueryWrapper<T> wrapper() {
            return this.wrapper;
        }

        // ===================== 核心：指定查询字段（不传则查全部） =====================
        @SafeVarargs
        public final WxLambdaChainQuery select(SFunction<T, ?>... columns) {
            if (columns != null && columns.length > 0) {
                wrapper.select(columns);
                hasSelectFields = true;
            }
            // 不传参数则不做处理，默认查全部字段
            return this;
        }

        // ===================== 常用 Lambda 条件 =====================

        public <R> WxLambdaChainQuery eq(SFunction<T, R> column, Object val) {
            wrapper.eq(column, val);
            return this;
        }

        public WxLambdaChainQuery entity(T t) {
            t.clearEmptyString();
            wrapper.setEntity(t);
            return this;
        }

        public <R> WxLambdaChainQuery ne(SFunction<T, R> column, Object val) {
            wrapper.ne(column, val);
            return this;
        }

        public <R> WxLambdaChainQuery like(SFunction<T, R> column, Object val) {
            wrapper.like(column, val);
            return this;
        }

        public <R> WxLambdaChainQuery orderByDesc(SFunction<T, R> column) {
            wrapper.orderByDesc(column);
            return this;
        }
        public <R> WxLambdaChainQuery orderByAsc(SFunction<T, R> column) {
            wrapper.orderByAsc(column);
            return this;
        }

        public <R> WxLambdaChainQuery ge(SFunction<T, R> column, Object val) {
            wrapper.ge(column, val);
            return this;
        }

        public <R> WxLambdaChainQuery le(SFunction<T, R> column, Object val) {
            wrapper.le(column, val);
            return this;
        }

        public <R> WxLambdaChainQuery in(SFunction<T, R> column, Collection<?> coll) {
            wrapper.in(column, coll);
            return this;
        }

        // ===================== 分页设置 =====================
        public WxLambdaChainQuery page(IPage<T> page) {
            this.page = page;
            return this;
        }

        // ===================== 执行查询 =====================
        public T one() {
            List<T> list = list();
            if (list.isEmpty()) return null;
            if (list.size() > 1) throw new RuntimeException("查询结果超过1条，请缩小查询范围");
            return list.get(0);
        }

        public List<T> list() {
            List<T> list = WxServiceImpl.this.list(wrapper);
            processEntityList(list);
            return list;
        }

        public IPage<T> page() {
            if (this.page == null) this.page = PageFactory.defaultPage();
            IPage<T> resultPage = WxServiceImpl.this.page(this.page, wrapper);
            processEntityList(resultPage.getRecords());
            return resultPage;
        }

        public long count() {
            return WxServiceImpl.this.count(wrapper);
        }
    }

    /**
     * 安排批量执行
     *
     * @param consumer item->{print(item)}
     */
    public void forEachPage(
            Consumer<T> consumer
    ) {
        this.forEachPage(new LambdaQueryWrapper<T>(), 100, consumer);
    }

    /**
     * 安排批量执行
     *
     * @param wrapper
     * @param consumer
     */
    public void forEachPage(
            Wrapper<T> wrapper,
            Consumer<T> consumer
    ) {
        this.forEachPage(wrapper, 100, consumer);
    }

    public void forEachPage(
            Wrapper<T> wrapper,
            int pageSize,
            Consumer<T> consumer
    ) {
        long current = 1;
        while (true) {
            IPage<T> page = new Page<>(current, pageSize);
            IPage<T> result = this.page(page, wrapper);
            List<T> records = result.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (T record : records) {
                consumer.accept(record);
            }
            if (records.size() < pageSize) {
                break; // 最后一页
            }
            current++;
        }
    }

    public LambdaUpdateWrapper<T> updateWrapper() {
        return new LambdaUpdateWrapper<T>();
    }

}