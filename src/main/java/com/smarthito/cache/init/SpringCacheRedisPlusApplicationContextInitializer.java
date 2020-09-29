package com.smarthito.cache.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 上下文初始化，在bean装配前加载
 *
 * @author yaojunguang
 */
public class SpringCacheRedisPlusApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, EnvironmentPostProcessor, Ordered {

    private static final String SPRING_CACHE_REDIS_PLUS_PROPERTY_SOURCES = "SpringCacheRedisPlusPropertySources";

    public SpringCacheRedisPlusApplicationContextInitializer() {
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        if (environment.getProperty("spring.cache.redis.plus.enabled", Boolean.class, true)) {
            this.initialize(environment);
        }
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty("spring.cache.redis.plus.enabled", Boolean.class, true)) {
            this.initialize(environment);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    protected void initialize(ConfigurableEnvironment environment) {


        if (!environment.getPropertySources().contains(SPRING_CACHE_REDIS_PLUS_PROPERTY_SOURCES)) {

        }
    }
}
