package com.limado.collab.util.converter;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * author flyman
 */
public class BeanPropertyConverter {

    private static final Logger log = LogManager.getLogger(BeanPropertyConverter.class);
    private static final ConvertUtilsBean converter = BeanUtilsBean.getInstance().getConvertUtils();
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

    public static <T> T convert(Object value, Class targetType) {
        T result = null;
        try {
            result = (T) converter.convert(value, targetType);
        } catch (org.apache.commons.beanutils.ConversionException e) {
            throw new ConversionException(String.format("convert failure from %s (%s) to %s", value.toString(), value.getClass().getName(), targetType.getName()), e);
        }
        return result;
    }

    // override default converter will throw an exception when passed null or an invalid value as its input
    static {
//        converter.register(new org.apache.commons.beanutils.converters.BooleanConverter(), Boolean.class);
//        converter.register(new org.apache.commons.beanutils.converters.ByteConverter(), Byte.class);
//        converter.register(new org.apache.commons.beanutils.converters.ShortConverter(), Short.class);
//        converter.register(new org.apache.commons.beanutils.converters.IntegerConverter(), Integer.class);
//        converter.register(new org.apache.commons.beanutils.converters.LongConverter(), Long.class);
//        converter.register(new org.apache.commons.beanutils.converters.FloatConverter(), Float.class);
//        converter.register(new org.apache.commons.beanutils.converters.DoubleConverter(), Double.class);
        converter.register(new UUIDConverter(), java.util.UUID.class);
        converter.register(new DateTimeConverter(), java.util.Date.class);
        converter.register(new DateTimeConverter(), java.sql.Timestamp.class);
    }

    private BeanPropertyConverter() {

    }

    private static class UUIDConverter extends org.apache.commons.beanutils.converters.AbstractConverter {

        private UUIDConverter() {
            super(null);
        }

        @Override
        protected String convertToString(Object value) throws Throwable {
            return value.toString();
        }

        @Override
        protected UUID convertToType(Class arg0, Object value) throws Throwable {
            return java.util.UUID.fromString((String) value);
        }

        @Override
        protected Class getDefaultType() {
            return java.util.UUID.class;
        }

    }

    private static class DateTimeConverter extends org.apache.commons.beanutils.converters.AbstractConverter {

        private DateTimeConverter() {
            super(null);
        }

        private DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        @Override
        protected String convertToString(Object value) throws Throwable {
            return dateFormat.format((Date) value);
        }

        @Override
        protected Date convertToType(Class type, Object value) throws Throwable {
            return dateFormat.parse((String) value);
        }

        @Override
        protected Class getDefaultType() {
            return java.util.Date.class;
        }

    }
}
