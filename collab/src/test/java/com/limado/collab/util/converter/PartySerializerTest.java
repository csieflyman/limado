/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.converter;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.limado.collab.model.Group;
import com.limado.collab.model.User;
import com.limado.collab.util.converter.json.JsonConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * @author csieflyman
 */
public class PartySerializerTest {

    private static final Logger log = LogManager.getLogger(PartySerializerTest.class);

    @Test
    public void testAvoidCycle() {
        Group group1 = new Group();
        group1.setId(UUID.randomUUID());
        group1.setIdentity("group1");

        Group group2 = new Group();
        group2.setId(UUID.randomUUID());
        group2.setIdentity("group2");
        Group group3 = new Group();
        group3.setId(UUID.randomUUID());
        group3.setIdentity("group3");

        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setIdentity("user1");
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setIdentity("user2");

        group2.addChild(group1);
        group3.addChild(group1);
        group1.addChild(user1);
        group1.addChild(user2);
        JsonConverter converter = JsonConverter.getInstance();
        String jsonObject = converter.convertOut(group1);

        Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject);
        List<String> parentsIdentityList = JsonPath.read(document, "$.parents[*].identity");
        List<String> childrenIdentityList = JsonPath.read(document, "$.children[*].identity");
        Assert.assertEquals(Sets.newHashSet("group2", "group3"), new HashSet(parentsIdentityList));
        Assert.assertEquals(Sets.newHashSet("user1", "user2"), new HashSet(childrenIdentityList));

        Assert.assertTrue(((List)JsonPath.read(document, "$.parents[0].parents")).isEmpty());
        Assert.assertTrue(((List)JsonPath.read(document, "$.parents[1].parents")).isEmpty());
        Assert.assertTrue(((List)JsonPath.read(document, "$.parents[0].children")).isEmpty());
        Assert.assertTrue(((List)JsonPath.read(document, "$.parents[1].children")).isEmpty());
        Assert.assertTrue(((List)JsonPath.read(document, "$.children[0].parents")).isEmpty());
        Assert.assertTrue(((List)JsonPath.read(document, "$.children[1].parents")).isEmpty());
        Assert.assertTrue(((List)JsonPath.read(document, "$.children[0].children")).isEmpty());
        Assert.assertTrue(((List)JsonPath.read(document, "$.children[1].children")).isEmpty());
    }
}
