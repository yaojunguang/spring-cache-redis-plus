package com.smarthito.cache.redis.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 必须重写序列化器，否则@Cacheable注解的key会报类型转换错误
 *
 * @author yaojunguang
 */
public class StringRedisSerializer implements RedisSerializer<String> {

    private final Charset charset;

    private final static String TARGET = "\"";

    private final static String REPLACEMENT = "";

    private final static String SIMPLE_KEY = "SimpleKey [";

    public StringRedisSerializer() {
        this(StandardCharsets.UTF_8);
    }

    public StringRedisSerializer(Charset charset) {
        Assert.notNull(charset, "Charset must not be null!");
        this.charset = charset;
    }

    @Override
    public String deserialize(byte[] bytes) {
        return (bytes == null ? null : new String(bytes, charset));
    }

    @Override
    public byte[] serialize(String object) {
        if (object == null) {
            return null;
        }
        //强制修改key为空的情况下的生成
        if (object.contains(SIMPLE_KEY)) {
            int index = object.lastIndexOf(":");
            if (index > 0) {
                object = object.substring(0, index + 1) + "{}";
            }
        }
        try {
            String string = new ObjectMapper().writeValueAsString(object);
            string = string.replace(TARGET, REPLACEMENT);
            return string.getBytes(charset);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
