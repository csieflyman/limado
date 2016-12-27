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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * author flyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml"})
public class UserServiceImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private Map<String, User> userMap;
    private Map<String, Group> groupMap;
    private Map<String, Organization> orgMap;

    @Autowired
    private UserService userService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private GroupService groupService;

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

    @Test
    public void testDelete() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));

        organizationService.addChildren(org1, Sets.newHashSet(user1, user2));
        Assert.assertEquals(Sets.newHashSet(user1, user2), organizationService.getDescendants(org1.getId()));
        userService.delete(user1);
        Assert.assertEquals(Sets.newHashSet(user2), organizationService.getDescendants(org1.getId()));
    }

    @Test
    public void testAddRemoveParents() {
        Group group1 = groupService.create(groupMap.get("group1"));
        Organization org1 = organizationService.create(orgMap.get("org1"));
        User user1 = userService.create(userMap.get("user1"));

        userService.addParents(user1, Sets.newHashSet(org1, group1));
        Assert.assertEquals(Sets.newHashSet(org1, group1), userService.getParents(user1.getId()));

        userService.removeParents(user1, Sets.newHashSet(org1, group1));
        Assert.assertEquals(Collections.emptySet(), userService.getParents(user1.getId()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addChild() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.addChild(user1, user2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeChild() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.removeChild(user1, user2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addChildren() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.addChildren(user1, Sets.newHashSet(user2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeChildren() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.removeChildren(user1, Sets.newHashSet(user2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserParents() {
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        userService.addParents(user1, Sets.newHashSet(user2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTwoOrganizationParents() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        User user1 = userService.create(userMap.get("user1"));
        userService.addParents(user1, Sets.newHashSet(org1, org2));
    }
}
