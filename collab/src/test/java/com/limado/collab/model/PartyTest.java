/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.model;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * author flyman
 */
public class PartyTest {

    private Map<String, Party> userMap;
    private Map<String, Party> groupMap;
    private Map<String, Party> orgMap;

    @Before
    public void initTestData() {
        Map<String, Map<String, Party>> partyMap = generateTestData();
        userMap = partyMap.get("user");
        groupMap = partyMap.get("group");
        orgMap = partyMap.get("org");
    }

    @Test
    public void testUserRelations() {
        User user = (User) userMap.get("user1");
        Assert.assertEquals(Sets.newHashSet(orgMap.get("org1"), groupMap.get("group1")), user.getParents());
        Assert.assertEquals(Sets.newHashSet(groupMap.get("group1")), user.getGroups());
        Assert.assertEquals(orgMap.get("org1"), user.getOrganization());
    }

    @Test
    public void testGroupRelations() {
        Group group = (Group) groupMap.get("group2");
        Assert.assertEquals(Sets.newHashSet(groupMap.get("group1")), group.getParents());
        Assert.assertEquals(Sets.newHashSet(groupMap.get("group1")), group.getSuperGroups());
        Assert.assertEquals(Sets.newHashSet(groupMap.get("group4"), orgMap.get("org2"), userMap.get("user2")), group.getChildren());
        Assert.assertEquals(Sets.newHashSet(groupMap.get("group4")), group.getSubGroups());
        Assert.assertEquals(Sets.newHashSet(orgMap.get("org2")), group.getOrganizations());
        Assert.assertEquals(Sets.newHashSet(userMap.get("user2")), group.getUsers());
    }

    @Test
    public void testOrganizationRelations() {
        Organization org = (Organization) orgMap.get("org2");
        Assert.assertEquals(Sets.newHashSet(orgMap.get("org1"), groupMap.get("group2")), org.getParents());
        Assert.assertEquals(orgMap.get("org1"), org.getSuperOrganization());
        Assert.assertEquals(Sets.newHashSet(orgMap.get("org4"), userMap.get("user2")), org.getChildren());
        Assert.assertEquals(Sets.newHashSet(orgMap.get("org4")), org.getSubOrganizations());
        Assert.assertEquals(Sets.newHashSet(userMap.get("user2")), org.getUsers());
    }

    private static Map<String, Map<String, Party>> generateTestData() {
        Map<String, Map<String, Party>> partyMap = new HashMap<>();
        Map<String, Party> userMap = generateUserData();
        partyMap.put("user", userMap);
        Map<String, Party> groupMap = generateGroupData();
        partyMap.put("group", groupMap);
        Map<String, Party> orgMap = generateOrgData();
        partyMap.put("org", orgMap);

        addOrgOrgData(orgMap);
        addOrgUserData(orgMap, userMap);
        addGroupGroupData(groupMap);
        addGroupUserData(groupMap, userMap);
        addGroupOrgData(groupMap, orgMap);
        return partyMap;
    }

    private static Map<String, Party> generateUserData() {
        Map<String, Party> userMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            String key = "user" + i;
            User user = new User(key);
            user.setName(key.toUpperCase());
            if(i == 5)
                user.setEnabled(false);
            userMap.put(key, user);
        }
        return userMap;
    }

    private static Map<String, Party> generateGroupData() {
        Map<String, Party> groupMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            String key = "group" + i;
            Group group = new Group(key);
            group.setName(key.toUpperCase());
            if(i == 5)
                group.setEnabled(false);
            groupMap.put(key, group);
        }

        return groupMap;
    }

    private static Map<String, Party> generateOrgData() {
        Map<String, Party> orgMap = new HashMap<>();
        for(int i = 1; i <= 5; i++) {
            String key = "org" + i;
            Organization org = new Organization(key);
            org.setName(key.toUpperCase());
            if(i == 5)
                org.setEnabled(false);
            orgMap.put(key, org);
        }
        return orgMap;
    }

    private static void addOrgOrgData(Map<String, Party> orgMap) {
        orgMap.get("org1").addChild(orgMap.get("org2"));
        orgMap.get("org1").addChild(orgMap.get("org3"));
        orgMap.get("org2").addChild(orgMap.get("org4"));
        orgMap.get("org3").addChild(orgMap.get("org5"));
    }

    private static void addOrgUserData(Map<String, Party> orgMap, Map<String, Party> userMap) {
        orgMap.get("org1").addChild(userMap.get("user1"));
        orgMap.get("org2").addChild(userMap.get("user2"));
        orgMap.get("org3").addChild(userMap.get("user3"));
        orgMap.get("org4").addChild(userMap.get("user4"));
        orgMap.get("org5").addChild(userMap.get("user5"));
    }

    private static void addGroupGroupData(Map<String, Party> groupMap) {
        groupMap.get("group1").addChild(groupMap.get("group2"));
        groupMap.get("group1").addChild(groupMap.get("group3"));
        groupMap.get("group2").addChild(groupMap.get("group4"));
        groupMap.get("group3").addChild(groupMap.get("group5"));
    }

    private static void addGroupUserData(Map<String, Party> groupMap, Map<String, Party> userMap) {
        groupMap.get("group1").addChild(userMap.get("user1"));
        groupMap.get("group2").addChild(userMap.get("user2"));
        groupMap.get("group3").addChild(userMap.get("user3"));
        groupMap.get("group4").addChild(userMap.get("user4"));
        groupMap.get("group5").addChild(userMap.get("user5"));
    }

    private static void addGroupOrgData(Map<String, Party> groupMap, Map<String, Party> orgMap) {
        groupMap.get("group1").addChild(orgMap.get("org1"));
        groupMap.get("group2").addChild(orgMap.get("org2"));
        groupMap.get("group3").addChild(orgMap.get("org3"));
        groupMap.get("group4").addChild(orgMap.get("org4"));
        groupMap.get("group5").addChild(orgMap.get("org5"));
    }
}
