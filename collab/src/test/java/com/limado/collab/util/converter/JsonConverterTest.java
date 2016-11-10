/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.converter;

import com.limado.collab.util.converter.json.JsonConverter;
import com.limado.collab.util.converter.json.JsonView;
import com.limado.collab.util.converter.json.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * author flyman
 */
public class JsonConverterTest {

    private static final Logger log = LogManager.getLogger(JsonConverterTest.class);

    @Test
    public void testJsonObject() {
        String dateTimeString = "2016-01-01T01:10:10.123+0800";
        Date dateTime = BeanPropertyConverter.convert(dateTimeString, Date.class);
        User user1 = new User(UUID.fromString("c9591a88-dcdc-4f82-93cb-10f9977f7d9b"), "user1", dateTime);
        User user2 = new User(UUID.fromString("74d87dc3-8945-4d6c-81f2-d78ca08c45c7"), "user2", dateTime);
        user1.setFriends(Arrays.asList(user2));

        JsonConverter converter = JsonConverter.getInstance();
        String jsonObject = converter.convertOut(JsonView.with(user1).onClass(User.class, Match.match().exclude("name")));
        Assert.assertTrue(!jsonObject.contains("name"));
        Assert.assertTrue(jsonObject.contains("c9591a88-dcdc-4f82-93cb-10f9977f7d9b"));
        Assert.assertTrue(jsonObject.contains("74d87dc3-8945-4d6c-81f2-d78ca08c45c7"));
        Assert.assertTrue(jsonObject.contains("2016-01-01T01:10:10.123+0800"));

        User user = converter.convertIn(jsonObject, User.class);
        Assert.assertThat(UUID.fromString("c9591a88-dcdc-4f82-93cb-10f9977f7d9b"), equalTo(user.getUuid()));
        Assert.assertThat(dateTime, equalTo(user.getDate()));
        Assert.assertTrue(user.getName() == null);
        Assert.assertTrue(user.getFriends().size() == 1);

        Map<String, Object> userMap = converter.convertInToMap(jsonObject, String.class, Object.class);
        Assert.assertTrue(userMap instanceof Map);
    }

    @Test
    public void testJsonArray() {
        User user1 = new User(UUID.fromString("c9591a88-dcdc-4f82-93cb-10f9977f7d9b"), "user1", null);
        User user2 = new User(UUID.fromString("74d87dc3-8945-4d6c-81f2-d78ca08c45c7"), "user2", null);
        List<User> userList = Arrays.asList(user1, user2);
        JsonConverter converter = JsonConverter.getInstance();
        String jsonArray = converter.convertOut(userList);

        List<User> userList1 = converter.convertInToList(jsonArray, User.class);
        Set<User> userSet1 = converter.convertInToSet(jsonArray, User.class);
        Assert.assertTrue(userList1 != null);
        Assert.assertTrue(userSet1 != null);
        Assert.assertTrue(userList1.size() == 2);
        Assert.assertTrue(userSet1.size() == 2);
    }

    private static class User {

        private UUID uuid;
        private String name;
        private Date date = new Date();
        private String type = "user";
        private List<User> friends;

        public User() {

        }

        User(UUID uuid, String name, Date date) {
            this.uuid = uuid;
            this.name = name;
            this.date = date;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<User> getFriends() {
            return friends;
        }

        public void setFriends(List<User> friends) {
            this.friends = friends;
        }

        @Override
        public String toString() {
            return "User{" +
                    "uuid=" + uuid +
                    ", name='" + name + '\'' +
                    ", date=" + date +
                    ", type='" + type + '\'' +
                    ", friends=" + friends +
                    '}';
        }
    }
}
