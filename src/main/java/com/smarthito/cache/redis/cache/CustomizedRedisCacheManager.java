package com.smarthito.cache.redis.cache;

import com.smarthito.cache.redis.utils.ReflectionUtils;
import com.smarthito.cache.redis.utils.SpringContextUtils;
import com.smarthito.cache.redis.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义的redis缓存管理器
 * 支持方法上配置过期时间
 * 支持热加载缓存：缓存即将过期时主动刷新缓存
 *
 * @author yaojunguang
 */
public class CustomizedRedisCacheManager extends RedisCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(CustomizedRedisCacheManager.class);

    /**
     * 父类cacheMap字段
     */
    private static final String SUPER_FIELD_CACHE_MAP = "cacheMap";

    /**
     * 父类allowInFlightCacheCreation字段
     */
    private static final String SUPER_FIELD_ALLOW_IN_FLIGHT_CACHE_CREATION = "allowInFlightCacheCreation";

    /**
     * 父类updateCacheNames方法
     */
    private static final String SUPER_METHOD_UPDATE_CACHE_NAMES = "updateCacheNames";

    /**
     * 缓存参数的分隔符
     * 数组元素0=缓存的名称
     * 数组元素1=缓存过期时间TTL
     * 数组元素2=缓存在多少秒开始主动失效来强制刷新
     */
    private static final String SEPARATOR = "#";

    /**
     * SpEL标示符
     */
    private static final String MARK = "$";

    RedisCacheManager redisCacheManager = null;

    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration config;
    private final RedisTemplate<String, Object> redisOperations;

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    public CustomizedRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration config, RedisTemplate<String, Object> redisOperations) {
        super(cacheWriter, config);
        this.cacheWriter = cacheWriter;
        this.config = config;
        this.redisOperations = redisOperations;
    }

    public RedisCacheManager getInstance() {
        if (redisCacheManager == null) {
            redisCacheManager = SpringContextUtils.getBean(RedisCacheManager.class);
        }
        return redisCacheManager;
    }

    @Override
    public Cache getCache(String name) {
        String[] cacheParams = name.split(SEPARATOR);
        String cacheName = cacheParams[0];

        if (StringUtil.isBlank(cacheName)) {
            return null;
        }

        // 有效时间，初始化获取默认的有效时间
        long expirationSecondTime = getExpirationSecondTime(cacheParams);
        // 自动刷新时间，默认是0
        long preloadSecondTime = getPreloadSecondTime(cacheParams);

        // 通过反射获取父类存放缓存的容器对象
        Object object = ReflectionUtils.getFieldValue(getInstance(), SUPER_FIELD_CACHE_MAP);
        if (object instanceof ConcurrentHashMap) {
            ConcurrentHashMap<String, Cache> cacheMap = (ConcurrentHashMap<String, Cache>) object;
            // 生成Cache对象，并将其保存到父类的Cache容器中
            return getCache(cacheName, expirationSecondTime, preloadSecondTime, cacheMap);
        } else {
            return super.getCache(cacheName);
        }

    }

    public long getExpirationSecondTime(String[] cacheParams) {
        //获取过期时间
        if (cacheParams == null || cacheParams.length == 0) {
            return 0;
        }

        // 有效时间，初始化获取默认的有效时间
        long expirationSecondTime = config.getTtl().getSeconds();

        // 设置key有效时间
        if (cacheParams.length > 1) {
            String expirationStr = cacheParams[1];
            if (!StringUtil.isBlank(expirationStr)) {
                // 支持配置过期时间使用EL表达式读取配置文件时间
                if (expirationStr.contains(MARK)) {
                    expirationStr = beanFactory.resolveEmbeddedValue(expirationStr);
                }
                expirationSecondTime = Long.parseLong(Objects.requireNonNull(expirationStr));
            }
        }

        return expirationSecondTime < 0 ? 0 : expirationSecondTime;
    }

    private long getPreloadSecondTime(String[] cacheParams) {
        //获取自动刷新时间
        // 自动刷新时间，默认是0
        long preloadSecondTime = 0L;
        // 设置自动刷新时间
        if (cacheParams.length > 2) {
            String preloadStr = cacheParams[2];
            if (!StringUtil.isBlank(preloadStr)) {
                // 支持配置刷新时间使用EL表达式读取配置文件时间
                if (preloadStr.contains(MARK)) {
                    preloadStr = beanFactory.resolveEmbeddedValue(preloadStr);
                }
                preloadSecondTime = Long.parseLong(Objects.requireNonNull(preloadStr));
            }
        }
        return preloadSecondTime < 0 ? 0 : preloadSecondTime;
    }

    /**
     * 重写父类的getCache方法，增加了三个参数
     *
     * @param cacheName            缓存名称
     * @param expirationSecondTime 过期时间
     * @param preloadSecondTime    自动刷新时间
     * @param cacheMap             通过反射获取的父类的cacheMap对象
     * @return Cache
     */
    public Cache getCache(String cacheName, long expirationSecondTime, long preloadSecondTime, ConcurrentHashMap<String, Cache> cacheMap) {
        Cache cache = cacheMap.get(cacheName);
        if (cache != null) {
            return cache;
        } else {
            // Fully synchronize now for missing cache creation...
            synchronized (cacheMap) {
                cache = cacheMap.get(cacheName);
                if (cache == null) {
                    // 调用我们自己的getMissingCache方法创建自己的cache
                    cache = getMissingCache(cacheName, expirationSecondTime, preloadSecondTime);
                    if (cache != null) {
                        cache = decorateCache(cache);
                        cacheMap.put(cacheName, cache);
                        // 反射去执行父类的updateCacheNames(cacheName)方法
                        Class<?>[] parameterTypes = {String.class};
                        Object[] parameters = {cacheName};
                        ReflectionUtils.invokeMethod(getInstance(), SUPER_METHOD_UPDATE_CACHE_NAMES, parameterTypes, parameters);
                    }
                }
                return cache;
            }
        }
    }

    /**
     * 创建缓存
     *
     * @param cacheName            缓存名称
     * @param expirationSecondTime 过期时间
     * @param preloadSecondTime    制动刷新时间
     * @return aa
     */
    public CustomizedRedisCache getMissingCache(String cacheName, long expirationSecondTime, long preloadSecondTime) {
        logger.info("缓存 cacheName：{}，过期时间:{}, 自动刷新时间:{}", cacheName, expirationSecondTime, preloadSecondTime);
        boolean allowInFlightCacheCreation = (boolean) ReflectionUtils.getFieldValue(getInstance(), SUPER_FIELD_ALLOW_IN_FLIGHT_CACHE_CREATION);
        return allowInFlightCacheCreation ? new CustomizedRedisCache(cacheName, cacheWriter, config.entryTtl(Duration.ofSeconds(expirationSecondTime))
                , redisOperations, preloadSecondTime) : null;
    }
}
