/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.converter.json;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.limado.collab.model.Party;
import com.limado.collab.util.converter.ConversionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author flyman
 *
 */
public class JsonConverter {

    private static final Logger log = LogManager.getLogger(JsonConverter.class);
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private static final JsonConverter instance = new JsonConverter();
    
    private JsonConverter() {
        init();
    }
    
    public static JsonConverter getInstance() {
        return instance;
    }
    
    public void init() {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(Include.ALWAYS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //mapper.getSerializerProvider().setNullValueSerializer(new NullToEmptyStringSerializer());
        //mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        registerModules();
    }
    
    private void registerModules(){
        SimpleModule module = new SimpleModule("default");
        module.addAbstractTypeMapping(List.class, ArrayList.class);
        module.addAbstractTypeMapping(Map.class, HashMap.class);
        module.addAbstractTypeMapping(Set.class, HashSet.class);
        module.addSerializer(Party.class, new PartySerializer());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"));
        mapper.registerModule(module);
        mapper.registerModule(new JodaModule());
        mapper.registerModule(new Hibernate5Module());
    }
    
    public ObjectMapper getObjectMapper() {
        return mapper;
    }
    
    public <T> List<T> convertInToList(String jsonString, Class<T> beanClass) {
        List<T> results;
        try {
            JavaType javaType = mapper.getTypeFactory().constructCollectionType(List.class, beanClass);
            results = mapper.readValue(jsonString, javaType);
        } catch (Exception e) {
            throw new ConversionException("fail to convert to " + beanClass.getName() + " : " + jsonString, e);
        }
        return results;
    }
    
    public <T> Set<T> convertInToSet(String jsonString, Class<T> beanClass) {
        Set<T> results;
        try {
            JavaType javaType = mapper.getTypeFactory().constructCollectionType(Set.class, beanClass);
            results = mapper.readValue(jsonString, javaType);
        } catch (Exception e) {
            throw new ConversionException("fail to convert to " + beanClass.getName() + " : " + jsonString, e);
        }
        return results;
    }
    
    public <key, value> Map<key, value> convertInToMap(String jsonString, Class<key> mapKeyClass, Class<value> mapValueClass) {
        Map<key, value> results;
        try {
            JavaType javaType = mapper.getTypeFactory().constructMapType(HashMap.class, mapKeyClass, mapValueClass);
            results = mapper.readValue(jsonString, javaType);
        } catch (Exception e) {
            throw new ConversionException("fail to convert to Map : " + jsonString, e);
        }
        return results;
    }
    
    public <T> T convertIn(String jsonString, Class<T> beanClass) {
        T bean;
        try {
            bean = mapper.readValue(jsonString, beanClass);
        } catch (Exception e) {
            throw new ConversionException("fail to convert to " + beanClass.getName() + " : " + jsonString, e);
        }
        return bean;
    }
    
    public String convertOut(Object bean) {
        String result;
        try {
            result = mapper.writeValueAsString(bean);
        } catch (JsonProcessingException e) {
            throw new ConversionException("fail to convert to json : " + bean, e);
        }
        return result;
    }
}
