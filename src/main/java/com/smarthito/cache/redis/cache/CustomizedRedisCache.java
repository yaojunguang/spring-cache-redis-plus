package com.smarthito.cache.redis.cache;

import com.smarthito.cache.redis.lock.RedisLock;
import com.smarthito.cache.redis.utils.SpringContextUtils;
import com.smarthito.cache.redis.utils.ThreadTaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Created by @author yangmingtian on 2020/1/8
 */
public class CustomizedRedisCache extends RedisCache {

    private static final Logger logger = LoggerFactory.getLogger(CustomizedRedisCache.class);

    private CacheSupport getCacheSupport() {
        return SpringContextUtils.getBean(CacheSupport.class);
    }

    /**
     * 缓存主动在失效前强制刷新缓存的时间
     * 单位：秒
     */
    private final long preloadSecondTime;
    private final RedisCacheConfiguration config;
    private final RedisTemplate<String, Object> redisOperations;

    protected CustomizedRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration config, RedisTemplate<String, Object> redisOperations, Long preloadSecondTime) {
        super(name, cacheWriter, config);
        this.config = config;
        this.redisOperations = redisOperations;
        // 指定自动刷新时间
        this.preloadSecondTime = preloadSecondTime;
    }


    /**
     * 重写get方法，获取到缓存后再次取缓存剩余的时间，如果时间小余我们配置的刷新时间就手动刷新缓存。
     * 为了不影响get的性能，启用后台线程去完成缓存的刷。
     * 并且只放一个线程去刷新数据。
     *
     * @param key 名称
     * @return 结果
     */
    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper valueWrapper = super.get(key);
        if (null != valueWrapper) {
            // 刷新缓存数据
            refreshCache(key, getCacheKey(key));
        }
        return valueWrapper;
    }

    private void refreshCache(Object key, String cacheKeyStr) {
        //刷新缓存数据
        Long ttl = this.redisOperations.getExpire(cacheKeyStr);
        if (null != ttl && ttl <= CustomizedRedisCache.this.preloadSecondTime) {
            // 尽量少的去开启线程，因为线程池是有限的
            ThreadTaskUtils.run(() -> {
                // 加一个分布式锁，只放一个请求去刷新缓存
                RedisLock redisLock = new RedisLock(redisOperations, cacheKeyStr + "_lock");
                try {
                    if (redisLock.lock()) {
                        // 获取锁之后再判断一下过期时间，看是否需要加载数据
                        Long ttl1 = CustomizedRedisCache.this.redisOperations.getExpire(cacheKeyStr);
                        if (null != ttl1 && ttl1 <= CustomizedRedisCache.this.preloadSecondTime) {
                            // 通过获取代理方法信息重新加载缓存数据
                            logger.info("refresh key:{}", key);
                            CustomizedRedisCache.this.getCacheSupport().refreshCacheByKey(CustomizedRedisCache.super.getName(), cacheKeyStr);
                        }
                    }
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                } finally {
                    redisLock.unlock();
                }
            });
        }
    }

    public long getExpirationSecondTime() {
        return config.getTtl().getSeconds();
    }

    public String getCacheKey(Object key) {
        return this.createCacheKey(key);
    }
}
