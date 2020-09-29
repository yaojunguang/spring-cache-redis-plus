package com.smarthito.cache.enable;

import com.smarthito.cache.redis.config.SpringCacheRedisPlusConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * redis标准化配置，需根据springboot默认格式配置redis连接后才能生效
 *
 * @author yaojunguang
 */
@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = {java.lang.annotation.ElementType.TYPE})
@Import({SpringCacheRedisPlusConfig.class})
public @interface EnableSpringCacheRedisPlusConfig {
}
