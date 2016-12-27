package com.limado.collab.service;

import com.google.common.collect.Sets;
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

import java.util.Collections;
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
        organizationService.addChild(org1, org2);
        organizationService.addChild(org2, org3);

        Set<Party> org1Children = organizationService.getChildren(org1.getId());
        Set<Party> org2Children = organizationService.getChildren(org2.getId());
        Assert.assertTrue(org1Children.contains(org2));
        Assert.assertTrue(org2Children.contains(org3));

        organizationService.movePartyToOrganization(org3, org1);
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
        organizationService.addChild(org1, org2);
        organizationService.addChild(org2, user1);

        Set<Party> org1Children = organizationService.getChildren(org1.getId());
        Set<Party> org2Children = organizationService.getChildren(org2.getId());
        Assert.assertTrue(org1Children.contains(org2));
        Assert.assertTrue(org2Children.contains(user1));

        organizationService.movePartyToOrganization(user1, org1);
        org1Children = organizationService.getChildren(org1.getId());
        org2Children = organizationService.getChildren(org2.getId());
        Assert.assertTrue(org1Children.contains(user1));
        Assert.assertFalse(org2Children.contains(user1));
    }

    @Test
    public void testAddAndRemoveChildren() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        Organization org3 = organizationService.create(orgMap.get("org3"));
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        User user3 = userService.create(userMap.get("user3"));

        organizationService.addChildren(org1, Sets.newHashSet(user1, user2));
        organizationService.addChild(org1, user3);
        Assert.assertEquals(Sets.newHashSet(user1, user2, user3), organizationService.getChildren(org1.getId()));

        organizationService.removeChildren(org1, Sets.newHashSet(user1, user2));
        organizationService.removeChild(org1, user3);
        Assert.assertEquals(Collections.emptySet(), organizationService.getChildren(org1.getId()));

        organizationService.addParents(org3, Sets.newHashSet(org2));
        Assert.assertEquals(Sets.newHashSet(org3), organizationService.getChildren(org2.getId()));
        organizationService.removeParents(org3, Sets.newHashSet(org2));
        Assert.assertEquals(Collections.emptySet(), organizationService.getChildren(org2.getId()));
    }

    @Test
    public void testDelete() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        Organization org3 = organizationService.create(orgMap.get("org3"));
        User user1 = userService.create(userMap.get("user1"));
        User user2 = userService.create(userMap.get("user2"));
        User user3 = userService.create(userMap.get("user3"));

        organizationService.addChildren(org1, Sets.newHashSet(org2, org3, user1));
        organizationService.addChildren(org2, Sets.newHashSet(user2));
        organizationService.addChildren(org3, Sets.newHashSet(user3));
        Assert.assertEquals(Sets.newHashSet(user1, org2, user2, org3, user3), organizationService.getDescendants(org1.getId()));

        organizationService.delete(org2);
        Assert.assertEquals(Sets.newHashSet(user1, org3, user3), organizationService.getDescendants(org1.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void moveGroupToOrganization() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Group group1 = groupService.create(groupMap.get("group1"));
        organizationService.movePartyToOrganization(group1, org1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addGroupChild() {
        Organization org1 = orgMap.get("org1");
        Group group1 = groupMap.get("group1");
        organizationService.addChild(org1, group1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChildWhenHaveTwoParents() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        User user1 = userService.create(userMap.get("user1"));
        organizationService.addChild(org1, user1);
        organizationService.addChild(org2, user1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChildWhenAlreadyExist() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        User user1 = userService.create(userMap.get("user1"));
        organizationService.addChild(org1, user1);
        organizationService.addChild(org1, user1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserParents() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        User user1 = userService.create(userMap.get("user1"));
        organizationService.addParents(org1, Sets.newHashSet(user1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTwoOrganizationParents() {
        Organization org1 = organizationService.create(orgMap.get("org1"));
        Organization org2 = organizationService.create(orgMap.get("org2"));
        Organization org3 = organizationService.create(orgMap.get("org3"));
        organizationService.addParents(org1, Sets.newHashSet(org2, org3));
    }
}
