/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.converter;

import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * author flyman
 */
public class BeanPropertyConverterTest {

    @Test
    public void testPrimitive() {

    }

    @Test
    public void testDate() {
        String dateTimeString = "2016-01-01T01:10:10.123+0800";
        Date dateTime = BeanPropertyConverter.convert(dateTimeString, Date.class);
        DateFormat dateFormat = new SimpleDateFormat(BeanPropertyConverter.DATE_FORMAT);
        String convertedDateTimeString = dateFormat.format(dateTime);
        Assert.assertThat(dateTimeString, equalTo(convertedDateTimeString));
    }

    @Test
    public void testUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        UUID convertedUUID = BeanPropertyConverter.convert(uuidString, UUID.class);
        Assert.assertThat(uuid, equalTo(convertedUUID));
    }
}
