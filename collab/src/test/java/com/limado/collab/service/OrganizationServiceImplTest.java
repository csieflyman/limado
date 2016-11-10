/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
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
import java.util.Set;

/**
 * author flyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml"})
public class OrganizationServiceImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private Map<String, User> userMap;
    private Map<String, Group> groupMap;
    private Map<String, Organization> orgMap;

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private UserService userService;
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
    public void moveOrganizationToOrganization() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        Organization org3 = organizationService.create(orgMap.get("org3"));
        organizationService.addChild(org1.getId(), org2.getId());
        organizationService.addChild(org2.getId(), org3.getId());

        Set<Party> org1Children = organizationService.getChildren(org1.getId());
        Set<Party> org2Children = organizationService.getChildren(org2.getId());
        Assert.assertTrue(org1Children.contains(org2));
        Assert.assertTrue(org2Children.contains(org3));

        organizationService.movePartyToOrganization(org3.getId(), org1.getId());
        org1Children = organizationService.getChildren(org1.getId());
        org2Children = organizationService.getChildren(org2.getId());
        Assert.assertTrue(org1Children.contains(org3));
        Assert.assertFalse(org2Children.contains(org3));
    }

    @Test
    public void moveUserToOrganization() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        User user1 = userService.create(userMap.get("user1"));
        organizationService.addChild(org1.getId(), org2.getId());
        organizationService.addChild(org2.getId(), user1.getId());

        Set<Party> org1Children = organizationService.getChildren(org1.getId());
        Set<Party> org2Children = organizationService.getChildren(org2.getId());
        Assert.assertTrue(org1Children.contains(org2));
        Assert.assertTrue(org2Children.contains(user1));

        organizationService.movePartyToOrganization(user1.getId(), org1.getId());
        org1Children = organizationService.getChildren(org1.getId());
        org2Children = organizationService.getChildren(org2.getId());
        Assert.assertTrue(org1Children.contains(user1));
        Assert.assertFalse(org2Children.contains(user1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void moveGroupToOrganization() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        Group group1 = groupService.create(groupMap.get("group1"));
        organizationService.movePartyToOrganization(group1.getId(), org1.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addGroupChild() {
        Organization org1 = orgMap.get("org1");
        Group group1 = groupMap.get("group1");
        organizationService.addChild(org1.getId(), group1.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChildWhenHaveTwoParents() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        User user1 = userService.create(userMap.get("user1"));
        organizationService.addChild(org1.getId(), user1.getId());
        organizationService.addChild(org2.getId(), user1.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChildWhenAlreadyExist() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        User user1 = userService.create(userMap.get("user1"));
        organizationService.addChild(org1.getId(), user1.getId());
        organizationService.addChild(org1.getId(), user1.getId());
    }
}
