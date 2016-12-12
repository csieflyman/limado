/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.collect.Sets;
import com.limado.collab.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * author flyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml"})
public class UserServiceImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private Map<String, User> userMap;

    @Autowired
    private UserService userService;

    @Before
    public void initTestData() {
        userMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            String identity = "user" + i;
            User user = new User(identity);
            user.setName(" I am " + identity);
            user.setEmail(identity + "@example.com");
            userMap.put(identity, user);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addChild() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.addChild(user1.getId(), user2.getId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeChild() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.removeChild(user1.getId(), user2.getId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addChildren() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.addChildren(user1.getId(), Sets.newHashSet(user2.getId()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeChildren() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.removeChildren(user1.getId(), Sets.newHashSet(user2.getId()));
    }
}
