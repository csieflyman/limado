/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.google.common.collect.Sets;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import com.limado.collab.util.converter.BeanPropertyConverter;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.OrderBy;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml"})
@DirtiesContext
public class PartyDaoImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final Logger log = LogManager.getLogger(PartyDaoImplTest.class);

    private Map<String, Party> userMap;
    private Map<String, Party> groupMap;
    private Map<String, Party> orgMap;

    @Autowired
    private PartyDao partyDao;

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
        Party user = partyDao.create(userMap.get("user1"));
        Assert.assertNotNull(user.getId());

        // retrieve
        user = partyDao.getById(user.getId());
        Assert.assertNotNull(user);

        // update
        User t_user = new User("user1");
        t_user.setId(user.getId());
        t_user.setName("user1 modified");
        partyDao.update(t_user);
        user = partyDao.getById(user.getId());
        Assert.assertEquals("user1 modified", user.getName());

        // delete
        partyDao.delete(user);
        user = partyDao.getById(user.getId());
        Assert.assertNull(user);
    }

    @Test
    public void testFindAndFindSize() {
        initTestDataForFind();
        Set<Party> allParties = new HashSet<>();
        allParties.addAll(userMap.values());
        allParties.addAll(orgMap.values());
        allParties.addAll(groupMap.values());

        // paging query and sort, type query
        QueryParams qp1 = new QueryParams();
        String predicate1 = "[TYPE(party) in (User,Group,Organization)]";
        qp1.put(QueryParams.Q_PREDICATES, predicate1);
        qp1.addOrderBy(new OrderBy("identity", false));
        qp1.setOffset(0);
        qp1.setLimit(allParties.size());
        List<Party> parties = partyDao.find(qp1);
        Collection<Party> expectedParties = allParties.stream().sorted(Comparator.comparing(Party::getIdentity).reversed()).collect(Collectors.toList());
        Assert.assertEquals(expectedParties, parties);
        int size = partyDao.findSize(qp1);
        Assert.assertEquals(expectedParties.size(), size);

        // type query
        QueryParams qp2 = new QueryParams();
        String predicate2 = "[TYPE(party) = User ; identity = user1]";
        qp2.put(QueryParams.Q_PREDICATES, predicate2);
        parties = partyDao.find(qp2);
        expectedParties = Sets.newHashSet(userMap.get("user1"));
        Assert.assertEquals(expectedParties,new HashSet<>(parties));
        size = partyDao.findSize(qp2);
        Assert.assertEquals(expectedParties.size(), size);

        // predicates with the same property, disjunction
        QueryParams qp3 = new QueryParams();
        String predicate3 = "[identity = user1 ; identity = user2]";
        qp3.put(QueryParams.Q_PREDICATES, predicate3);
        qp3.setPredicatesDisjunction(true);
        parties = partyDao.find(qp3);
        expectedParties = Sets.newHashSet(userMap.get("user1"), userMap.get("user2"));
        Assert.assertEquals(expectedParties, new HashSet<>(parties));
        size = partyDao.findSize(qp3);
        Assert.assertEquals(expectedParties.size(), size);

        // like operator
        QueryParams qp4 = new QueryParams();
        String predicate4 = "[identity like user%]";
        qp4.put(QueryParams.Q_PREDICATES, predicate4);
        parties = partyDao.find(qp4);
        expectedParties = new HashSet<>(userMap.values());
        Assert.assertEquals(expectedParties, new HashSet<>(parties));
        size = partyDao.findSize(qp4);
        Assert.assertEquals(expectedParties.size(), size);

        // in operator
        QueryParams qp5 = new QueryParams();
        String predicate5 = "[identity in (user1,group1,org1)]";
        qp5.put(QueryParams.Q_PREDICATES, predicate5);
        parties = partyDao.find(qp5);
        expectedParties = Sets.newHashSet(userMap.get("user1"), groupMap.get("group1"), orgMap.get("org1"));
        Assert.assertEquals(expectedParties, new HashSet<>(parties));
        size = partyDao.findSize(qp5);
        Assert.assertEquals(expectedParties.size(), size);

        // date value restriction
        QueryParams qp6 = new QueryParams();
        Date currentTimestamp = new Date();
        DateFormat dateFormat = new SimpleDateFormat(BeanPropertyConverter.DATE_FORMAT);
        String timeString = dateFormat.format(currentTimestamp);
        String predicate6 = "[creationDate <= " + timeString + "]";
        qp6.put(QueryParams.Q_PREDICATES, predicate6);
        parties = partyDao.find(qp6);
        expectedParties = allParties;
        Assert.assertEquals(expectedParties, new HashSet<>(parties));
        size = partyDao.findSize(qp6);
        Assert.assertEquals(expectedParties.size(), size);

        // boolean value restriction
        QueryParams qp7 = new QueryParams();
        String predicate7 = "[enabled = true]";
        qp7.put(QueryParams.Q_PREDICATES, predicate7);
        parties = partyDao.find(qp7);
        expectedParties = allParties;
        Assert.assertEquals(expectedParties, new HashSet<>(parties));
        size = partyDao.findSize(qp7);
        Assert.assertEquals(expectedParties.size(), size);

        // uuid value restriction and fetch parents, children relation
        QueryParams qp8 = new QueryParams();
        String predicate8 = "[id = " + orgMap.get("org1").getId() + "]";
        qp8.put(QueryParams.Q_PREDICATES, predicate8);
        qp8.setFetchRelations(Sets.newHashSet(Party.RELATION_PARENT, Party.RELATION_CHILDREN));
        parties = partyDao.find(qp8);
        Assert.assertEquals(1, parties.size());
        Assert.assertEquals(orgMap.get("org1"), parties.get(0));
        Assert.assertEquals(getParents(orgMap.get("org1")), parties.get(0).getParents());
        Assert.assertEquals(getChildren(orgMap.get("org1")), parties.get(0).getChildren());
        size = partyDao.findSize(qp8);
        Assert.assertEquals(1, size);

        // fetch parent relation
        QueryParams qp9 = new QueryParams();
        String predicate9 = "[parents.id = " + orgMap.get("org1").getId() + " ; TYPE(party) = User]";
        qp9.put(QueryParams.Q_PREDICATES, predicate9);
        qp9.setFetchRelations(Sets.newHashSet(Party.RELATION_PARENT));
        parties = partyDao.find(qp9);
        Assert.assertEquals(1, parties.size());
        Assert.assertEquals(userMap.get("user1"), parties.get(0));
        Assert.assertEquals(getParents(userMap.get("user1")), parties.get(0).getParents());
        size = partyDao.findSize(qp9);
        Assert.assertEquals(1, size);

        // fetch children relation
        QueryParams qp10 = new QueryParams();
        String predicate10 = "[children.id = " + userMap.get("user1").getId() + " ; TYPE(party) = Organization]";
        qp10.put(QueryParams.Q_PREDICATES, predicate10);
        qp10.setFetchRelations(Sets.newHashSet(Party.RELATION_CHILDREN));
        parties = partyDao.find(qp10);
        Assert.assertEquals(1, parties.size());
        Assert.assertEquals(orgMap.get("org1"), parties.get(0));
        Assert.assertEquals(getChildren(orgMap.get("org1")), parties.get(0).getChildren());
        size = partyDao.findSize(qp10);
        Assert.assertEquals(1, size);

        // children, parent relation restriction
        QueryParams qp11 = new QueryParams();
        String predicate11 = "[children.id = " + userMap.get("user2").getId() + " ; parents.id = " + orgMap.get("org1").getId() + "]";
        qp11.put(QueryParams.Q_PREDICATES, predicate11);
        parties = partyDao.find(qp11);
        Assert.assertEquals(orgMap.get("org2"), parties.get(0));
        size = partyDao.findSize(qp11);
        Assert.assertEquals(1, size);

        QueryParams qp12 = new QueryParams();
        String predicate12 = "[children is_null]";
        qp12.put(QueryParams.Q_PREDICATES, predicate12);
        parties = partyDao.find(qp12);
        Assert.assertEquals(Sets.newHashSet(userMap.get("user1"), userMap.get("user2"), userMap.get("user3")), new HashSet(parties));

        QueryParams qp13 = new QueryParams();
        String predicate13 = "[parents is_null]";
        qp13.put(QueryParams.Q_PREDICATES, predicate13);
        parties = partyDao.find(qp13);
        Assert.assertEquals(Sets.newHashSet(groupMap.get("group1")), new HashSet(parties));
    }

    @Test
    public void testBatchCRUD() {
        Collection<Party> parties = userMap.values();
        // create
        partyDao.batchCreate(parties);
        parties = partyDao.find(new QueryParams());
        parties.forEach(party -> Assert.assertNotNull(party.getId()));

        // update
        parties.forEach(party -> party.setEnabled(false));
        partyDao.batchUpdate(parties);
        parties = partyDao.find(new QueryParams());
        parties.forEach(party -> Assert.assertFalse(party.getEnabled()));

        // bulk update
        Set<UUID> ids = parties.stream().map(Party::getId).collect(Collectors.toSet());
        Map<String, Object> updatedValueMap = new HashMap<>();
        updatedValueMap.put("enabled", true);
        partyDao.batchUpdate(ids, updatedValueMap);
        parties = partyDao.find(new QueryParams());
        parties.forEach(party -> Assert.assertTrue(party.getEnabled()));

        // delete
        partyDao.batchDelete(parties);
        parties = partyDao.find(new QueryParams());
        Assert.assertEquals(0, parties.size());

        // create
        partyDao.batchCreate(parties);
        parties = partyDao.find(new QueryParams());
        parties.forEach(party -> Assert.assertNotNull(party.getId()));

        // delete
        ids = parties.stream().map(Party::getId).collect(Collectors.toSet());
        partyDao.batchDeleteById(ids);
        parties = partyDao.find(new QueryParams());
        Assert.assertEquals(0, parties.size());
    }

    @Test
    public void testAddRemoveParentsAndChildren() {
        userMap = userMap.values().stream().map(user -> partyDao.create(user)).collect(Collectors.toMap(Party::getIdentity, user -> user));
        groupMap = groupMap.values().stream().map(group -> partyDao.create(group)).collect(Collectors.toMap(Party::getIdentity, user -> user));
        orgMap = orgMap.values().stream().map(org -> partyDao.create(org)).collect(Collectors.toMap(Party::getIdentity, user -> user));

        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");
        Party user3 = userMap.get("user3");
        Party org1 = orgMap.get("org1");
        Party org3 = orgMap.get("org3");
        Party group3 = groupMap.get("group3");

        partyDao.addChildren(org1, Sets.newHashSet(user1, user2));
        Assert.assertEquals(Sets.newHashSet(user1, user2), getChildren(org1));
        partyDao.addChild(org1, user3);
        Assert.assertEquals(Sets.newHashSet(user1, user2, user3), getChildren(org1));

        partyDao.removeChildren(org1, Sets.newHashSet(user1, user2));
        Assert.assertEquals(Sets.newHashSet(user3), getChildren(org1));
        partyDao.removeChild(org1, user3);
        Assert.assertEquals(new HashSet<>(), getChildren(org1));

        partyDao.addParents(user3, Sets.newHashSet(org3, group3));
        Assert.assertEquals(Sets.newHashSet(group3, org3), getParents(user3));
        partyDao.removeParents(user3, Sets.newHashSet(org3, group3));
        Assert.assertEquals(new HashSet<>(), getParents(user3));
    }

    private Set<Party> getParents(Party party) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.EQ, party.getId()));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_PARENT));
        List<Party> parties = partyDao.find(params);
        Assert.assertTrue(parties.size() == 1);
        return parties.get(0).getParents();
    }

    private Set<Party> getChildren(Party party) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.EQ, party.getId()));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_CHILDREN));
        List<Party> parties = partyDao.find(params);
        Assert.assertTrue(parties.size() == 1);
        return parties.get(0).getChildren();
    }

    private void initTestDataForFind() {
        userMap = userMap.values().stream().map(user -> partyDao.create(user)).collect(Collectors.toMap(Party::getIdentity, user -> user));
        groupMap = groupMap.values().stream().map(group -> partyDao.create(group)).collect(Collectors.toMap(Party::getIdentity, user -> user));
        orgMap = orgMap.values().stream().map(org -> partyDao.create(org)).collect(Collectors.toMap(Party::getIdentity, user -> user));

        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");
        Party user3 = userMap.get("user3");
        Party org1 = orgMap.get("org1");
        Party org2 = orgMap.get("org2");
        Party org3 = orgMap.get("org3");
        Party group1 = groupMap.get("group1");
        Party group2 = groupMap.get("group2");
        Party group3 = groupMap.get("group3");

        partyDao.addChild(group1, group2);
        partyDao.addChild(group1, group3);
        partyDao.addChild(group1, org1);
        partyDao.addChild(group2, org2);
        partyDao.addChild(group3, org3);
        partyDao.addChild(group1, user1);
        partyDao.addChild(group2, user2);
        partyDao.addChild(group3, user3);

        partyDao.addChild(org1, org2);
        partyDao.addChild(org2, org3);
        partyDao.addChild(org1, user1);
        partyDao.addChild(org2, user2);
        partyDao.addChild(org3, user3);
    }
}
