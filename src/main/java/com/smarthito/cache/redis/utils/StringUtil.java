package com.smarthito.cache.redis.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author yaojunguang
 * 一些方法的对象
 * Created by yaojunguang on 15/5/28.
 */
public class StringUtil {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    /**
     * UUID 去-
     *
     * @return 结果
     */
    public static String uuid() {
        String s = UUID.randomUUID().toString();
        return s.replace("-", "");
    }


    /**
     * 是否为空或者null
     *
     * @param value 查看值
     * @return 结果
     */
    public static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
