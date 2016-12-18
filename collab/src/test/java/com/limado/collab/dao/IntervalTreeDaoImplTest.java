/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
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
 * @author csieflyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml"})
@DirtiesContext
public class IntervalTreeDaoImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final Logger log = LogManager.getLogger(IntervalTreeDaoImplTest.class);

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    @Qualifier("partyIntervalTreeDao")
    private IntervalTreeDao<UUID> intervalTreeDao;

    @Autowired
    private PartyDao partyDao;

    private Map<String, Party> userMap;
    private Map<String, Party> orgMap;
    private UUID org1Id;
    private UUID org2Id;
    private UUID org3Id;
    private UUID org4Id;
    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;
    private UUID user4Id;
    private UUID user5Id;
    private UUID user6Id;

    @Before
    public void initTestData() {
        userMap = new HashMap<>();
        for (int i = 1; i <= 6; i++) {
            String identity = "user" + i;
            User user = new User(identity);
            user.setName(" I am " + identity);
            userMap.put(identity, user);
        }
        orgMap = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String identity = "org" + i;
            Organization org = new Organization(identity);
            org.setName(" I am " + identity);
            orgMap.put(identity, org);
        }
        userMap = userMap.values().stream().map(user -> partyDao.create(user)).collect(Collectors.toMap(Party::getIdentity, user -> user));
        orgMap = orgMap.values().stream().map(org -> partyDao.create(org)).collect(Collectors.toMap(Party::getIdentity, user -> user));

        org1Id = orgMap.get("org1").getId();
        org2Id = orgMap.get("org2").getId();
        org3Id = orgMap.get("org3").getId();
        org4Id = orgMap.get("org4").getId();
        user1Id = userMap.get("user1").getId();
        user2Id = userMap.get("user2").getId();
        user3Id = userMap.get("user3").getId();
        user4Id = userMap.get("user4").getId();
        user5Id = userMap.get("user5").getId();
        user6Id = userMap.get("user6").getId();
    }

    @Test
    public void testAddRemoveChild() {
        intervalTreeDao.addChild(org1Id, org2Id);
        intervalTreeDao.addChild(org1Id, org3Id);
        intervalTreeDao.addChild(org1Id, org4Id);
        intervalTreeDao.addChild(org2Id, user1Id);
        intervalTreeDao.addChild(org2Id, user2Id);
        intervalTreeDao.addChild(org3Id, user3Id);
        intervalTreeDao.addChild(org3Id, user4Id);
        intervalTreeDao.addChild(org4Id, user5Id);
        intervalTreeDao.addChild(org4Id, user6Id);

        List<UUID> subTreeNodeIds = intervalTreeDao.getSubTree(org1Id);
        Assert.assertEquals(Arrays.asList(org2Id, user1Id, user2Id, org3Id, user3Id, user4Id, org4Id, user5Id, user6Id), subTreeNodeIds);
        subTreeNodeIds = intervalTreeDao.getSubTree(org2Id);
        Assert.assertEquals(Arrays.asList(user1Id, user2Id), subTreeNodeIds);
        subTreeNodeIds = intervalTreeDao.getSubTree(user1Id);
        Assert.assertEquals(Collections.emptyList(), subTreeNodeIds);

        intervalTreeDao.removeChild(org2Id, user1Id);
        subTreeNodeIds = intervalTreeDao.getSubTree(org2Id);
        Assert.assertEquals(Arrays.asList(user2Id), subTreeNodeIds);

        intervalTreeDao.removeChild(org1Id, org3Id);
        subTreeNodeIds = intervalTreeDao.getSubTree(org1Id);
        Assert.assertEquals(Arrays.asList(org2Id, user2Id, org4Id, user5Id, user6Id), subTreeNodeIds);
        subTreeNodeIds = intervalTreeDao.getSubTree(org3Id);
        Assert.assertEquals(Arrays.asList(user3Id, user4Id), subTreeNodeIds);

        intervalTreeDao.removeChild(org1Id, org2Id);
        intervalTreeDao.removeChild(org2Id, user2Id);
        subTreeNodeIds = intervalTreeDao.getSubTree(org2Id);
        Assert.assertEquals(Collections.emptyList(), subTreeNodeIds);
        subTreeNodeIds = intervalTreeDao.getSubTree(org1Id);
        Assert.assertEquals(Arrays.asList(org4Id, user5Id, user6Id), subTreeNodeIds);
    }

    @Test
    public void testDeleteNode() {
        intervalTreeDao.addChild(org1Id, org2Id);
        intervalTreeDao.addChild(org1Id, org3Id);
        intervalTreeDao.addChild(org1Id, org4Id);
        intervalTreeDao.addChild(org2Id, user1Id);
        intervalTreeDao.addChild(org2Id, user2Id);
        intervalTreeDao.addChild(org3Id, user3Id);
        intervalTreeDao.addChild(org3Id, user4Id);
        intervalTreeDao.addChild(org4Id, user5Id);
        intervalTreeDao.addChild(org4Id, user6Id);

        intervalTreeDao.delete(org2Id);
        List<UUID> subTreeNodeIds = intervalTreeDao.getSubTree(org2Id);
        Assert.assertEquals(Collections.emptyList(), subTreeNodeIds);
        subTreeNodeIds = intervalTreeDao.getSubTree(org1Id);
        Assert.assertEquals(Arrays.asList(org3Id, user3Id, user4Id, org4Id, user5Id, user6Id), subTreeNodeIds);

        intervalTreeDao.delete(user3Id);
        subTreeNodeIds = intervalTreeDao.getSubTree(org3Id);
        Assert.assertEquals(Arrays.asList(user4Id), subTreeNodeIds);

        intervalTreeDao.delete(org1Id);
        subTreeNodeIds = intervalTreeDao.getSubTree(org4Id);
        Assert.assertEquals(Arrays.asList(user5Id, user6Id), subTreeNodeIds);
        subTreeNodeIds = intervalTreeDao.getSubTree(org1Id);
        Assert.assertEquals(Collections.emptyList(), subTreeNodeIds);
    }

    @Test
    public void testMoveNode() {
        intervalTreeDao.addChild(org1Id, org2Id);
        intervalTreeDao.addChild(org1Id, org3Id);
        intervalTreeDao.addChild(org1Id, org4Id);
        intervalTreeDao.addChild(org2Id, user1Id);
        intervalTreeDao.addChild(org2Id, user2Id);
        intervalTreeDao.addChild(org3Id, user3Id);
        intervalTreeDao.addChild(org3Id, user4Id);
        intervalTreeDao.addChild(org4Id, user5Id);
        intervalTreeDao.addChild(org4Id, user6Id);

        intervalTreeDao.move(org2Id, org3Id);
        List<UUID> subTreeNodeIds = intervalTreeDao.getSubTree(org2Id);
        Assert.assertEquals(Arrays.asList(user1Id, user2Id, org3Id, user3Id, user4Id), subTreeNodeIds);

        intervalTreeDao.move(org4Id, org2Id);
        subTreeNodeIds = intervalTreeDao.getSubTree(org1Id);
        Assert.assertEquals(Arrays.asList(org4Id, user5Id, user6Id, org2Id, user1Id, user2Id, org3Id, user3Id, user4Id), subTreeNodeIds);

        intervalTreeDao.move(org2Id, user3Id);
        subTreeNodeIds = intervalTreeDao.getSubTree(org2Id);
        Assert.assertEquals(Arrays.asList(user1Id, user2Id, org3Id, user4Id, user3Id), subTreeNodeIds);
    }
}
