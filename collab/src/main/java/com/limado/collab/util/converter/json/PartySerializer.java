package com.limado.collab.util.converter.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.limado.collab.model.Party;
import com.limado.collab.util.converter.ConversionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.collection.internal.PersistentSet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author csieflyman
 */
class PartySerializer extends JsonSerializer<Party> {

    private static final Logger log = LogManager.getLogger(PartySerializer.class);

    private static final ThreadLocal depthLocal = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            return 0;
        }
    };

    @Override
    public void serialize(Party party, JsonGenerator gen, SerializerProvider provider) throws IOException {
        int depth = (int) depthLocal.get();
        gen.writeStartObject();

        if (party == null) {
            provider.getDefaultNullValueSerializer().serialize(null, gen, provider);
        } else {
            Class cls = party.getClass();
            while (!cls.equals(Object.class)) {
                Field[] fields = cls.getDeclaredFields();
                for (Field field : fields) {
                    try {
                        field.setAccessible(true);
                        String name = field.getName();
                        Object value = field.get(party);

                        if (Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }

                        gen.writeFieldName(name);

                        if (name.equals(Party.RELATION_PARENT)) {
                            // we want serialize empty array instead of null
                            if (value == null || (value.getClass() == PersistentSet.class && !((PersistentSet) value).wasInitialized())) {
                                gen.writeStartArray();
                                gen.writeEndArray();
                            } else {
                                if (depth == 0) {
                                    depth++;
                                    depthLocal.set(depth);
                                    JsonSerializer serializer = provider.findValueSerializer(value.getClass());
                                    serializer.serialize(value, gen, provider);
                                    depth--;
                                    depthLocal.set(depth);
                                } else {
                                    gen.writeStartArray();
                                    gen.writeEndArray();
                                }
                            }
                        } else if (name.equals(Party.RELATION_CHILDREN)) {
                            if (value == null || (value.getClass() == PersistentSet.class && !((PersistentSet) value).wasInitialized())) {
                                gen.writeStartArray();
                                gen.writeEndArray();
                            } else {
                                if (depth == 0) {
                                    depth++;
                                    depthLocal.set(depth);
                                    JsonSerializer serializer = provider.findValueSerializer(value.getClass());
                                    serializer.serialize(value, gen, provider);
                                    depth--;
                                    depthLocal.set(depth);
                                } else {
                                    gen.writeStartArray();
                                    gen.writeEndArray();
                                }
                            }
                        } else {
                            if (value == null) {
                                provider.getDefaultNullValueSerializer().serialize(null, gen, provider);
                            } else {
                                provider.defaultSerializeValue(value, gen);
                            }
                        }
                    } catch (Throwable e) {
                        depthLocal.remove();
                        log.error("serialize" + party + " failure", e);
                        throw new ConversionException("serialize" + party + " failure", e);
                    }
                }
                cls = cls.getSuperclass();
            }
        }

        gen.writeEndObject();

        if (depth == 0) {
            depthLocal.remove();
        }
    }

    @Override
    public void serializeWithType(Party party, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        serialize(party, gen, serializers);
    }
}