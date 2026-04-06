package org.wx.core.wxBase.factory;

import cn.hutool.core.convert.Convert;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RedisFactory {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /* ==================== String ==================== */

    public void setBuySeconds(String key, Object value, long time) {
        redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
    }
    public void setBuyDay(String key, Object value, long time) {
        setBuyHour(key,value,time*24);
    }
    public void setBuyHour(String key, Object value, long time) {
        setBuyMinute(key,value,time*60);
    }
    public void setBuyMinute(String key, Object value, long time) {
        setBuySeconds(key,value,time*60);
    }

    public Boolean autoValidation(String key, Integer max) {
        Integer value = this.get(key, Integer.class);
        if(value==null){
            this.setBuyMinute(key,1,max);
            return false;
        }else {
            value++;
            this.setBuyMinute(key,value,max);
        }

        return value>max;
    }


    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T>T get(String key,Class<T> eClass) {
        Object object = redisTemplate.opsForValue().get(key);
        if(object==null){
            return null;
        }
        return Convert.convert(eClass, object);
    }

    public String getStr(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public Long incr(String key, long seconds) {
        Long val = stringRedisTemplate.opsForValue().increment(key);
        if (val != null && val == 1 && seconds > 0) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(seconds));
        }
        return val;
    }

    public void del(String key) {
        redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        Boolean b = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(b);
    }

    /* ==================== Hash ==================== */

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /* ==================== Lock（简单分布式锁） ==================== */

    public boolean tryLock(String key, long seconds) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", seconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
