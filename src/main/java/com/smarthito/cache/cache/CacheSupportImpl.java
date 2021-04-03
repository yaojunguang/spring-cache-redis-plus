package com.smarthito.cache.cache;

import com.smarthito.cache.cache.expression.CacheOperationExpressionEvaluator;
import com.smarthito.cache.utils.ReflectionUtils;
import com.smarthito.cache.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MethodInvoker;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 手动刷新缓存实现类
 *
 * @author yaojunguang
 */
@Slf4j
@Component("cacheSupport")
public class CacheSupportImpl implements CacheSupport {

    private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

    private static final String SEPARATOR = "#";

    private static final String INVOCATION_CACHE_KEY_SUFFIX = ":invocation_cache_key_suffix";

    @Resource
    private KeyGenerator keyGenerator;
    @Resource
    private RedisCacheManager cacheManager;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void registerInvocation(Object targetBean,
                                   Method targetMethod,
                                   Class[] invocationParamTypes,
                                   Object[] invocationArgs,
                                   Set<String> annotatedCacheNames,
                                   String cacheKey) {

        // 获取注解上真实的value值
        Collection<? extends Cache> caches = getCache(annotatedCacheNames);

        // 获取key的SpEL值
        Object key = generateKey(caches, cacheKey, targetMethod, invocationArgs, targetBean, CacheOperationExpressionEvaluator.NO_RESULT);

        // 新建一个代理对象（记录了缓存注解的方法类信息）
        final CachedMethodInvocation invocation = new CachedMethodInvocation(key, targetBean, targetMethod, invocationParamTypes, invocationArgs);
        for (Cache cache : caches) {
            if (cache instanceof CustomizedRedisCache) {
                CustomizedRedisCache redisCache = ((CustomizedRedisCache) cache);
                // 将方法信息放到redis缓存
                redisTemplate.opsForValue().set(getInvocationCacheKey(redisCache.getCacheKey(key)),
                        invocation, redisCache.getExpirationSecondTime(), TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void refreshCacheByKey(String cacheName, String cacheKey) {
        //在redis拿到方法信息，然后刷新缓存
        CachedMethodInvocation invocation = (CachedMethodInvocation) redisTemplate.opsForValue().get(getInvocationCacheKey(cacheKey));
        if (invocation != null) {
            // 执行刷新方法
            refreshCache(invocation, cacheName);
        }

    }

    private void refreshCache(CachedMethodInvocation invocation, String cacheName) {
        try {
            // 通过代理调用方法，并记录返回值
            Object computed = invoke(invocation);

            // 通过cacheManager获取操作缓存的cache对象
            Cache cache = cacheManager.getCache(cacheName);
            // 通过Cache对象更新缓存
            assert cache != null;
            cache.put(invocation.getKey(), computed);
            CustomizedRedisCache redisCache = (CustomizedRedisCache) cache;
            long expireTime = redisCache.getExpirationSecondTime();
            // 刷新redis中缓存法信息key的有效时间
            redisTemplate.expire(getInvocationCacheKey(redisCache.getCacheKey(invocation.getKey())), expireTime, TimeUnit.SECONDS);

            log.info("缓存：{}-{}，重新加载数据", cacheName, invocation.getKey().toString().getBytes());
        } catch (Exception e) {
            log.info("刷新缓存失败：" + e.getMessage(), e);
        }

    }

    private Object invoke(CachedMethodInvocation invocation) throws Exception {

        // 获取执行方法所需要的参数
        Object[] args = null;
        if (!CollectionUtils.isEmpty(invocation.getArguments())) {
            args = invocation.getArguments().toArray();
        }
        // 通过先获取Spring的代理对象，在根据这个对象获取真实的实例对象
        Object target = ReflectionUtils.getTarget(SpringContextUtils.getBean(Class.forName(invocation.getTargetBean())));
        final MethodInvoker invoker = new MethodInvoker();
        invoker.setTargetObject(target);
        invoker.setArguments(args);
        invoker.setTargetMethod(invocation.getTargetMethod());
        invoker.prepare();

        return invoker.invoke();
    }

    /**
     * 解析SpEL表达式，获取注解上的key属性值 直接扣的Spring解析表达式部分代码
     *
     * @param caches f
     * @param key    a
     * @param method a
     * @param args   a
     * @param target a
     * @param result a
     * @return a
     */
    protected Object generateKey(Collection<? extends Cache> caches, String key, Method method, Object[] args,
                                 Object target, Object result) {

        // 获取注解上的key属性值
        Class<?> targetClass = getTargetClass(target);
        if (org.springframework.util.StringUtils.hasText(key)) {
            EvaluationContext evaluationContext = evaluator.createEvaluationContext(caches, method, args, target,
                    targetClass, result, null);

            AnnotatedElementKey methodCacheKey = new AnnotatedElementKey(method, targetClass);
            return evaluator.key(key, methodCacheKey, evaluationContext);
        }
        return this.keyGenerator.generate(target, method, args);
    }

    private Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null && target != null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }


    public Collection<? extends Cache> getCache(Set<String> annotatedCacheNames) {
        //通过cache名称获取cache列表
        Collection<String> cacheNames = generateValue(annotatedCacheNames);

        if (cacheNames == null) {
            return Collections.emptyList();
        } else {
            Collection<Cache> result = new ArrayList<Cache>();
            for (String cacheName : cacheNames) {
                Cache cache = this.cacheManager.getCache(cacheName);
                if (cache == null) {
                    throw new IllegalArgumentException("Cannot find cache named '" + cacheName + "' for ");
                }
                result.add(cache);
            }
            return result;
        }
    }

    private String getInvocationCacheKey(String cacheKey) {
        return cacheKey + INVOCATION_CACHE_KEY_SUFFIX;
    }

    private Collection<String> generateValue(Set<String> annotatedCacheNames) {
        //获取注解上的value属性值（cacheNames）
        Collection<String> cacheNames = new HashSet<>();
        for (final String cacheName : annotatedCacheNames) {
            String[] cacheParams = cacheName.split(SEPARATOR);
            // 截取名称获取真实的value值
            String realCacheName = cacheParams[0];
            cacheNames.add(realCacheName);
        }
        return cacheNames;
    }
}
