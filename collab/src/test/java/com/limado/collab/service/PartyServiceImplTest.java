/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.collect.Sets;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml"})
@DirtiesContext
public class PartyServiceImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final Logger log = LogManager.getLogger(PartyServiceImplTest.class);

    private Map<String, Party> userMap;
    private Map<String, Party> orgMap;
    private Map<String, Party> groupMap;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    @Qualifier("partyService")
    private PartyService<Party> partyService;

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
    public void testCRUD() {
        // create
        Party user = partyService.create(userMap.get("user1"));
        Assert.assertNotNull(user.getId());

        // retrieve
        user = partyService.getById(user.getId());
        Assert.assertNotNull(user);
        user = new User();
        user.setIdentity("user1");
        user = partyService.get(user);
        Assert.assertNotNull(user);

        // update
        user.setName("user1 modified");
        partyService.update(user);
        user = partyService.getById(user.getId());
        Assert.assertEquals("user1 modified", user.getName());

        // delete
        partyService.deleteById(user.getId());
        Assert.assertFalse(partyService.checkExist(user.getType(), user.getIdentity()));
    }

    @Test
    public void createWithRelations() {
        Party org1 = orgMap.get("org1");
        Party org2 = orgMap.get("org2");
        Party user1 = userMap.get("user1");

        org1 = partyService.create(org1);
        user1 = partyService.create(user1);
        entityManager.flush();
        org2.setParents(Sets.newHashSet(org1));
        org2.setChildren(Sets.newHashSet(user1));
        org2 = partyService.create(org2);

        Assert.assertNotNull(org2.getId());
        Assert.assertEquals(Sets.newHashSet(org1), partyService.getParents(org2.getId()));
        Assert.assertEquals(Sets.newHashSet(user1), partyService.getChildren(org2.getId()));
    }

    @Test(expected=IllegalArgumentException.class)
    public void createDuplicatedTypeAndIdentity() {
        partyService.create(userMap.get("user1"));
        partyService.create(userMap.get("user1"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void updatDuplicatedTypeAndIdentity() {
        partyService.create(userMap.get("user1"));
        Party user2 = partyService.create(userMap.get("user2"));
        User detachUser2 = new User();
        detachUser2.setId(user2.getId());
        detachUser2.setIdentity("user1");
        partyService.update(detachUser2);
    }

    @Test
    public void updateWithoutRelations() {
        Party org1 = orgMap.get("org1");
        Party org2 = orgMap.get("org2");
        Party user1 = userMap.get("user1");

        org1 = partyService.create(org1);
        org2 = partyService.create(org2);
        user1 = partyService.create(user1);
        partyService.addChild(org1.getId(), org2.getId());
        partyService.addChild(org2.getId(), user1.getId());
        Assert.assertEquals(Sets.newHashSet(org1), partyService.getParents(org2.getId()));
        Assert.assertEquals(Sets.newHashSet(user1), partyService.getChildren(org2.getId()));

        Party newOrg2 = new Organization("org2");
        newOrg2.setId(org2.getId());
        newOrg2.setName("org2 modified");
        newOrg2.setParents(null);
        newOrg2.setChildren(null);
        partyService.update(newOrg2);

        org2 = partyService.getById(org2.getId());
        Assert.assertEquals(newOrg2.getName(), org2.getName());
        Assert.assertEquals(Sets.newHashSet(org1), partyService.getParents(org2.getId()));
        Assert.assertEquals(Sets.newHashSet(user1), partyService.getChildren(org2.getId()));
    }

    @Test
    public void updateWithRelations() {
        Party org1 = orgMap.get("org1");
        Party org2 = orgMap.get("org2");
        Party org3 = orgMap.get("org3");
        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");

        org1 = partyService.create(org1);
        org2 = partyService.create(org2);
        org3 = partyService.create(org3);
        user1 = partyService.create(user1);
        user2 = partyService.create(user2);
        partyService.addChild(org1.getId(), org2.getId());
        partyService.addChild(org2.getId(), user1.getId());
        Assert.assertEquals(Sets.newHashSet(org1), partyService.getParents(org2.getId()));
        Assert.assertEquals(Sets.newHashSet(user1), partyService.getChildren(org2.getId()));

        Party newOrg2 = new Organization("org2");
        newOrg2.setId(org2.getId());
        newOrg2.setName("org2 modified");
        newOrg2.setParents(Sets.newHashSet(org3));
        newOrg2.setChildren(Sets.newHashSet(user2));
        partyService.update(newOrg2);

        org2 = partyService.getById(org2.getId());
        Assert.assertEquals(newOrg2.getName(), org2.getName());
        Assert.assertEquals(Sets.newHashSet(org3), partyService.getParents(org2.getId()));
        Assert.assertEquals(Sets.newHashSet(user2), partyService.getChildren(org2.getId()));
    }

    @Test
    public void getById() {
        Party org1 = partyService.create(orgMap.get("org1"));
        Party org2 = partyService.create(orgMap.get("org2"));
        Party org3 = partyService.create(orgMap.get("org3"));
        partyService.addChild(org1.getId(), org2.getId());
        partyService.addChild(org2.getId(), org3.getId());
        org2 = partyService.getById(org2.getId(), Party.RELATION_PARENT, Party.RELATION_CHILDREN);
        Assert.assertNotNull(org2);
        log.debug(org2.getParents());
        log.debug(org2.getChildren());
        Assert.assertTrue(org2.getParents().contains(org1));
        Assert.assertTrue(org2.getChildren().contains(org3));
    }

    @Test
    public void checkExist() {
        Party user1 = partyService.create(userMap.get("user1"));
        Assert.assertTrue(partyService.checkExist(user1.getType(), user1.getIdentity()));
    }

    @Test
    public void deleteById() {
        Party org1 = orgMap.get("org1");
        Party org2 = orgMap.get("org2");
        Party user1 = userMap.get("user1");

        org1 = partyService.create(org1);
        org2 = partyService.create(org2);
        user1 = partyService.create(user1);
        partyService.addChild(org1.getId(), org2.getId());
        partyService.addChild(org2.getId(), user1.getId());

        partyService.deleteById(org2.getId());
        Assert.assertFalse(partyService.checkExist(org2.getType(), org2.getIdentity()));
        Assert.assertEquals(partyService.getChildren(org1.getId()), Collections.emptySet());
        Assert.assertEquals(partyService.getParents(user1.getId()), Collections.emptySet());
    }

    @Test
    public void deleteByIds() {
        Party org1 = orgMap.get("org1");
        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");
        Party user3 = userMap.get("user3");

        org1 = partyService.create(org1);
        Set<UUID> userIds = userMap.values().stream().map(user -> partyService.create(user)).map(Party::getId).collect(Collectors.toSet());
        partyService.addChild(org1.getId(), user1.getId());
        partyService.addChild(org1.getId(), user2.getId());
        partyService.addChild(org1.getId(), user3.getId());

        partyService.deleteByIds(userIds);
        List<Party> parties = partyService.find(new QueryParams());
        Assert.assertEquals(Sets.newHashSet(org1), new HashSet<>(parties));
        Assert.assertEquals(partyService.getChildren(org1.getId()), Collections.emptySet());
    }

    @Test
    public void testFindAndFindSize() {
        userMap = userMap.values().stream().map(user -> partyService.create(user)).collect(Collectors.toMap(Party::getIdentity, user -> user));
        QueryParams params = new QueryParams();
        List<Party> parties = partyService.find(params);
        Assert.assertEquals(new HashSet<>(userMap.values()), new HashSet<>(parties));
        int size = partyService.findSize(params);
        Assert.assertEquals(userMap.size(), size);
    }

    @Test
    public void testEnableAndDisable() {
        userMap.values().forEach(user -> user.setEnabled(false));
        Set<UUID> uuids = userMap.values().stream().map(user -> partyService.create(user)).map(Party::getId).collect(Collectors.toSet());
        //enable
        partyService.enable(uuids);
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("enabled", Operator.EQ, "true"));
        int size = partyService.findSize(params);
        Assert.assertEquals(userMap.size(), size);

        //disable
        partyService.disable(uuids);
        params = new QueryParams();
        params.addPredicate(new Predicate("enabled", Operator.EQ, "false"));
        size = partyService.findSize(params);
        Assert.assertEquals(userMap.size(), size);
    }

    @Test
    public void testParentsAndChildren() {
        Party user1 = partyService.create(userMap.get("user1"));
        Party user2 = partyService.create(userMap.get("user2"));
        Party org1 = partyService.create(orgMap.get("org1"));
        Party group1 = partyService.create(groupMap.get("group1"));
        Party group2 = partyService.create(groupMap.get("group2"));

        // add
        partyService.addChild(org1.getId(), user1.getId());
        partyService.addChild(org1.getId(), user2.getId());
        partyService.addChild(group1.getId(), org1.getId());
        partyService.addChild(group1.getId(), user1.getId());
        partyService.addChild(group1.getId(), user2.getId());
        partyService.addChild(group2.getId(), org1.getId());
        partyService.addChild(group2.getId(), user1.getId());
        partyService.addChild(group2.getId(), user2.getId());

        // retrieve children
        Set<Party> children = partyService.getChildren(org1.getId());
        Assert.assertEquals(Sets.newHashSet(user1, user2), children);
        children = partyService.getChildren(group1.getId());
        Assert.assertEquals(Sets.newHashSet(org1, user1, user2), children);
        // retrieve parents
        Set<Party> parents = partyService.getParents(user1.getId());
        Assert.assertEquals(Sets.newHashSet(org1, group1, group2), parents);
        parents = partyService.getParents(org1.getId());
        Assert.assertEquals(Sets.newHashSet(group1, group2), parents);

        // remove
        partyService.removeChild(org1.getId(), user1.getId());
        children = partyService.getChildren(org1.getId());
        Assert.assertFalse(children.contains(user1));
        parents = partyService.getParents(user1.getId());
        Assert.assertFalse(parents.contains(org1));
    }
}
