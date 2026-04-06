package org.wx.core.wxBase.aop;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.SneakyThrows;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.exception.WxApiException;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.factory.RedisFactory;

import jakarta.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RedisLockAop 拦截器（基于自定义 RedisFactory 实现）
 * 保证分布式锁获取与释放完整性，避免多线程并发导致的数据不一致。
 *
 * @author 无心
 */
@Aspect
@Component
public class RedisLockAop {

    // 注入你自定义的 RedisFactory
    @Resource
    private RedisFactory redisFactory;

    // 锁默认过期时间（30秒，避免死锁）
    private static final long LOCK_EXPIRE_SECONDS = 30L;
    // 锁重试间隔（100毫秒）
    private static final long LOCK_RETRY_INTERVAL = 100L;
    // 最大重试次数（30次 = 3秒）
    private static final int MAX_RETRY_COUNT = 30;

    @Pointcut(value = "@annotation(org.wx.core.wxBase.annotation.RedisLock)")
    public void Pointcut() {}

    /**
     * 构建分布式锁的唯一Key
     */
    public String getKey(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = ((MethodSignature) signature);
        Method method = methodSignature.getMethod();
        RedisLock annotation = method.getAnnotation(RedisLock.class);
        String baseKey = annotation.key();
        List<String> keysToBuild;

        // 特殊逻辑：baseKey为"e"时，取参数对象的主键字段
        if ("e".equals(baseKey)) {
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = methodSignature.getParameterNames();
            JSONObject paramJson = new JSONObject();
            for (int i = 0; i < args.length; i++) {
                paramJson.put(parameterNames[i], args[i]);
            }
            Object targetObject = paramJson.get(baseKey);
            if (targetObject != null) {
                String pkField = getPrimaryKeyFieldName(targetObject.getClass());
                if (pkField == null) {
                    ErrorFactory.redisLockError("Redis锁异常；未找到主键字段（@TableId）");
                }
                baseKey = baseKey + "." + pkField;
            } else {
                ErrorFactory.redisLockError("Redis锁异常；未找到参数对象：" + baseKey);
            }
        }

        // 拆分key规则，构建最终锁key
        keysToBuild = Arrays.asList(baseKey.split(","));
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = methodSignature.getParameterNames();
        JSONObject paramJson = new JSONObject();
        for (int i = 0; i < args.length; i++) {
            Object val = args[i];
            String key = parameterNames[i];
            paramJson.put(key, val);
        }

        HashMap<String, String> lockMap = new HashMap<>();
        keysToBuild.forEach(item -> {
            List<String> levelKeys = Arrays.asList(item.split("\\."));
            String val = paramJson.toJSONString();
            String errorKey = "";
            for (int i = 0; i < levelKeys.size(); i++) {
                errorKey += (i == 0 ? "" : ".") + levelKeys.get(i);
                val = JSONObject.parseObject(val).getString(levelKeys.get(i));
                if (val == null) {
                    String msg = String.format("Redis锁异常;获取方法参数[%s]失败;值不能为空;", errorKey);
                    ErrorFactory.redisLockError(msg);
                }
            }
            lockMap.put(item, val);
        });

        // 拼接完整锁key（类名.方法名 + 业务参数）
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethod = className + "." + methodName;
        String lockKey = String.join("_", lockMap.values());

        // 是否绑定方法名到锁key
        if (annotation.bindMethod()) {
            lockKey = fullMethod + ":::" + lockKey;
        }

        return lockKey;
    }

    /**
     * 获取实体类的主键字段名（支持父类）
     */
    public String getPrimaryKeyFieldName(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(TableId.class)) {
                return field.getName();
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return getPrimaryKeyFieldName(superClass);
        }
        return null;
    }

    /**
     * 前置通知：获取分布式锁
     */
    @SneakyThrows
    @Before("Pointcut()")
    public void doBefore(JoinPoint joinPoint) {
        String lockKey = getKey(joinPoint);
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RedisLock redisLock = method.getAnnotation(RedisLock.class);

        // 尝试获取锁
        boolean isLocked = redisFactory.tryLock(lockKey, LOCK_EXPIRE_SECONDS);

        // 开启重试机制
        if (!isLocked && redisLock.loading()) {
            int retryCount = 0;
            while (!isLocked && retryCount++ < MAX_RETRY_COUNT) {
                Thread.sleep(LOCK_RETRY_INTERVAL);
                isLocked = redisFactory.tryLock(lockKey, LOCK_EXPIRE_SECONDS);
            }
        }

        // 仍未获取到锁，抛出异常
        if (!isLocked) {
            ErrorFactory.redisLockError();
        }
    }

    /**
     * 环绕通知：执行目标方法（仅透传，无额外逻辑）
     */
    @Around("Pointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    /**
     * 后置通知：空实现（释放锁逻辑在 AfterReturning/AfterThrowing 中）
     */
    @After("Pointcut()")
    public void doAfter() {}

    /**
     * 正常返回后释放锁
     */
    @AfterReturning("Pointcut()")
    public void doAfterReturning(JoinPoint joinPoint) {
        String lockKey = getKey(joinPoint);
        try {
            redisFactory.unlock(lockKey);
            // System.err.println("分布式锁释放成功: " + lockKey);
        } catch (Exception e) {
            // System.err.println("分布式锁释放失败: " + lockKey + ", 原因: " + e.getMessage());
        }
    }

    /**
     * 异常抛出后释放锁（特殊异常除外）
     */
    @AfterThrowing(value = "Pointcut()", throwing = "ex")
    public void doAfterThrowing(JoinPoint joinPoint, Exception ex) {
        System.err.println("方法执行异常，开始处理分布式锁释放逻辑");
        boolean needUnlock = true;

        // 特殊异常：6379 不释放锁
        if (ex instanceof WxApiException exception) {
            if ("6379".equals(exception.getCode())) {
                needUnlock = false;
            }
        }

        // 释放锁
        if (needUnlock) {
            String lockKey = getKey(joinPoint);
            try {
                redisFactory.unlock(lockKey);
                // System.err.println("异常场景下分布式锁释放成功: " + lockKey);
            } catch (Exception ignored) {
                // 静默处理释放失败，避免影响主流程
            }
        }
    }
}