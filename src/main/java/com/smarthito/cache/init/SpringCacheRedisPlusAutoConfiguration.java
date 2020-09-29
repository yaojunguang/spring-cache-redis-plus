package com.smarthito.cache.init;

import com.smarthito.cache.enable.EnableSpringCacheRedisPlusConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;


/**
 * @author yaojunguang
 */
@Configuration
@EnableSpringCacheRedisPlusConfig
@ConditionalOnProperty(name = "spring.cache.redis.plus.enabled", matchIfMissing = true)
public class SpringCacheRedisPlusAutoConfiguration {
}