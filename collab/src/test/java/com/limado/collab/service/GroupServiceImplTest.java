/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.collect.Sets;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.User;
import org.junit.Assert;
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
public class GroupServiceImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private Map<String, User> userMap;
    private Map<String, Organization> orgMap;
    private Map<String, Group> groupMap;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

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
        orgMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            String identity = "org" + i;
            Organization org = new Organization(identity);
            org.setName(" I am " + identity);
            org.setEmail(identity + "@example.com");
            orgMap.put(identity, org);
        }
        groupMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            String identity = "group" + i;
            Group group = new Group(identity);
            group.setName(" I am " + identity);
            group.setEmail(identity + "@example.com");
            groupMap.put(identity, group);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserParents() {
        Group group1 = groupService.create(groupMap.get("group1"));
        User user1 = userService.create(userMap.get("user1"));
        groupService.addParents(group1, Sets.newHashSet(user1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addOrganizationParents() {
        Group group1 = groupService.create(groupMap.get("group1"));
        Organization org1 = organizationService.create(orgMap.get("org1"));
        groupService.addParents(group1, Sets.newHashSet(org1));
    }
}
