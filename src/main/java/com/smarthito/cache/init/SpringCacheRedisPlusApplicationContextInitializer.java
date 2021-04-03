package com.smarthito.cache.init;

import com.smarthito.cache.utils.SpringContextUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 上下文初始化，在bean装配前加载
 *
 * @author yaojunguang
 */
public class SpringCacheRedisPlusApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

        SpringContextUtils.setApplicationContext(configurableApplicationContext);
    }

}
