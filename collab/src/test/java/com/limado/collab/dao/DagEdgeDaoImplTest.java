package com.limado.collab.dao;

import com.google.common.collect.Sets;
import com.limado.collab.model.Group;
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

import java.util.Set;
import java.util.UUID;

/**
 * @author csieflyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml"})
@DirtiesContext
public class DagEdgeDaoImplTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final Logger log = LogManager.getLogger(DagEdgeDaoImplTest.class);

    @Autowired
    @Qualifier("partyDagEdgeDao")
    private DagEdgeDao<UUID> dagEdgeDao;

    @Autowired
    private PartyDao partyDao;

    private UUID group1Id;
    private UUID group2Id;
    private UUID group3Id;
    private UUID user4Id;
    private UUID user5Id;
    private UUID user6Id;
    private UUID user7Id;

    @Before
    public void initTestData() {
        Party group1 = new Group("group1");
        group1.setName("G1");
        group1 = partyDao.create(group1);
        group1Id = group1.getId();
        Party group2 = new Group("group2");
        group2.setName("G2");
        group2 = partyDao.create(group2);
        group2Id = group2.getId();
        Party group3 = new Group("group3");
        group3.setName("G3");
        group3 = partyDao.create(group3);
        group3Id = group3.getId();

        Party user4 = new User("user4");
        user4.setName("U4");
        user4 = partyDao.create(user4);
        user4Id = user4.getId();
        Party user5 = new User("user5");
        user5.setName("U5");
        user5 = partyDao.create(user5);
        user5Id = user5.getId();
        Party user6 = new User("user6");
        user6.setName("U6");
        user6 = partyDao.create(user6);
        user6Id = user6.getId();
        Party user7 = new User("user7");
        user7.setName("U7");
        user7 = partyDao.create(user7);
        user7Id = user7.getId();
    }

    @Test
    public void testAddRemoveEdges() {
        dagEdgeDao.addEdges(group1Id, group2Id);
        dagEdgeDao.addEdges(group1Id, group3Id);
        dagEdgeDao.addEdges(group2Id, user4Id);
        dagEdgeDao.addEdges(group2Id, user5Id);
        dagEdgeDao.addEdges(group3Id, user6Id);
        dagEdgeDao.addEdges(group3Id, user7Id);

        Set<UUID> incomingVertices = dagEdgeDao.findIncomingVertices(user4Id);
        Assert.assertEquals(Sets.newHashSet(group1Id, group2Id), incomingVertices);
        Set<UUID> outgoingVertices = dagEdgeDao.findOutgoingVertices(group1Id);
        Assert.assertEquals(Sets.newHashSet(group2Id, group3Id, user4Id, user5Id, user6Id, user7Id), outgoingVertices);

        dagEdgeDao.removeEdges(group2Id, user4Id);
        outgoingVertices = dagEdgeDao.findOutgoingVertices(group2Id);
        Assert.assertEquals(Sets.newHashSet(user5Id), outgoingVertices);
        incomingVertices = dagEdgeDao.findIncomingVertices(user4Id);
        Assert.assertEquals(Sets.newHashSet(), incomingVertices);

        dagEdgeDao.removeEdges(group1Id, group3Id);
        outgoingVertices = dagEdgeDao.findOutgoingVertices(group1Id);
        Assert.assertEquals(Sets.newHashSet(group2Id, user5Id), outgoingVertices);
        incomingVertices = dagEdgeDao.findIncomingVertices(user7Id);
        Assert.assertEquals(Sets.newHashSet(group3Id), incomingVertices);
    }

    @Test
    public void testRemoveEdgesOfVertex() {
        dagEdgeDao.addEdges(group1Id, group2Id);
        dagEdgeDao.addEdges(group1Id, group3Id);
        dagEdgeDao.addEdges(group2Id, user4Id);
        dagEdgeDao.addEdges(group2Id, user5Id);
        dagEdgeDao.addEdges(group3Id, user6Id);
        dagEdgeDao.addEdges(group3Id, user7Id);

        dagEdgeDao.removeEdgesOfVertex(group3Id);
        Set<UUID> outgoingVertices = dagEdgeDao.findOutgoingVertices(group1Id);
        Assert.assertEquals(Sets.newHashSet(group2Id, user4Id, user5Id), outgoingVertices);

        dagEdgeDao.removeEdgesOfVertex(group1Id);
        Set<UUID> incomingVertices = dagEdgeDao.findIncomingVertices(user4Id);
        Assert.assertEquals(Sets.newHashSet(group2Id), incomingVertices);
    }
}
