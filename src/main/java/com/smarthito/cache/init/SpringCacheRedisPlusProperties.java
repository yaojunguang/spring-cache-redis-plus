package com.smarthito.cache.init;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author yaojunguang at 2021/4/3 11:45 上午
 */

@Data
@Component
@ConditionalOnProperty(name = "spring.cache.redis.plus.enabled", havingValue = "true")
public class SpringCacheRedisPlusProperties {

    /**
     * 是否开启
     */
    private Boolean enabled = false;

    /**
     * 默认过期时间
     */
    private Integer expiration = 3600;
}
