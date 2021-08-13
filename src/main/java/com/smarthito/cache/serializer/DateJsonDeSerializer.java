package com.smarthito.cache.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.SneakyThrows;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yaojunguang
 */
public class DateJsonDeSerializer extends JsonDeserializer<Date> {

    @SneakyThrows
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String s = p.readValueAs(String.class);
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return dateTimeFormatter.parse(s);
    }
}
