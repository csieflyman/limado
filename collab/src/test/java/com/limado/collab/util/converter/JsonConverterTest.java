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
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * author flyman
 */
public class JsonConverterTest {

    @Test
    public void testJsonObject() {
        JsonConverter converter = JsonConverter.getInstance();

        Group group1 = new Group();
        String uuidString = "74d87dc3-8945-4d6c-81f2-d78ca08c45c7";
        UUID uuid = UUID.fromString(uuidString);
        group1.setId(uuid);
        group1.setIdentity("group1");
        group1.setName("Group1");
        String jsonObject = converter.convertOut(group1);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject);
        Assert.assertEquals(uuidString, JsonPath.read(document, "$.id"));
        Assert.assertEquals("group1", JsonPath.read(document, "$.identity"));
        Assert.assertEquals("Group1", JsonPath.read(document, "$.name"));

        Group group2 = converter.convertIn(jsonObject, Group.class);
        Assert.assertEquals(uuid, group2.getId());
        Assert.assertEquals("group1", group2.getIdentity());
        Assert.assertEquals("Group1", group2.getName());

        Map<String, Object> userMap = converter.convertInToMap(jsonObject, String.class, Object.class);
        Assert.assertTrue(userMap instanceof Map);
    }

    @Test
    public void testJsonArray() {
        JsonConverter converter = JsonConverter.getInstance();

        User user1 = new User();
        user1.setIdentity("user1");
        User user2 = new User();
        user2.setIdentity("user2");
        List<User> userList = Arrays.asList(user1, user2);
        String jsonArray = converter.convertOut(userList);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonArray);
        List<String> identityList = JsonPath.read(document, "$[*].identity");
        Assert.assertEquals(Sets.newHashSet("user1", "user2"), new HashSet(identityList));

        List<User> userList1 = converter.convertInToList(jsonArray, User.class);
        Set<User> userSet1 = converter.convertInToSet(jsonArray, User.class);
        Assert.assertTrue(userList1 != null);
        Assert.assertTrue(userSet1 != null);
        Assert.assertEquals(userList, userList1);
        Assert.assertEquals(Sets.newHashSet(userList), userSet1);
    }
}