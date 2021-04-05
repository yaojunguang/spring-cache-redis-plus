package com.smarthito.cache.generator;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 生成的工具
 *
 * @author yaojunguang at 2021/4/3 11:42 上午
 */

@Component("hyphenKeyGenerator")
public class HyphenKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object o, Method method, Object... objects) {
        if (objects.length > 0) {
            return Arrays.stream(objects).map(item -> {
                if (item == null) {
                    return "NULL";
                } else {
                    return item.toString();
                }
            }).collect(Collectors.joining("-"));
        }
        return "{}";
    }
}
