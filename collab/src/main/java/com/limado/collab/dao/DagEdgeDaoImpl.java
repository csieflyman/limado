/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */
package com.limado.collab.dao;

import com.google.common.base.Preconditions;
import com.limado.collab.model.DagEdge;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Query;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

abstract class DagEdgeDaoImpl<DagEdgeType extends DagEdge<VertexID>, VertexID extends Serializable> extends JpaGenericDaoImpl<DagEdgeType, Long> implements DagEdgeDao<VertexID> {

    private static final Logger log = LogManager.getLogger(DagEdgeDaoImpl.class);

    @Override
    public void addEdges(VertexID startVertexId, VertexID endVertexId) {
        Preconditions.checkArgument(startVertexId != null, "Argument [startVertexId] can not be null.");
        Preconditions.checkArgument(endVertexId != null, "Argument [endVertexId] can not be null.");
        log.debug(String.format("add edge: from %s to %s ", startVertexId, endVertexId));

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("startVertexId", Operator.EQ, startVertexId));
        params.addPredicate(new Predicate("endVertexId", Operator.EQ, endVertexId));
        params.addPredicate(new Predicate("hops", Operator.EQ, 0));
        int size = findSize(params);
        if (size > 0) {
            throw new IllegalArgumentException(String.format("edge from %s to %s has been exist", startVertexId, endVertexId));
        }

        Set incomingVertices = findIncomingVertices(startVertexId);
        boolean hasCycle = incomingVertices.contains(endVertexId);
        if(hasCycle){
            throw new IllegalArgumentException(String.format("add edge from %s to %s will cause cycle", startVertexId, endVertexId));
        }

        // insert direct edge
        DagEdge edge = newDagEdge();
        edge.setStartVertexId(startVertexId);
        edge.setEndVertexId(endVertexId);
        entityManager.persist(edge);
        Long id = edge.getId();
        log.debug("edge id : " + edge.getId());

        edge.setEntryEdgeId(id);
        edge.setDirectEdgeId(id);
        edge.setExitEdgeId(id);
        entityManager.merge(edge);

        //step 1: A's incoming edges to B
        StringBuilder insertSQL1 = new StringBuilder();
        insertSQL1.append("insert into ").append(getDagEdgeEntityName()).append("(entryEdgeId, directEdgeId, exitEdgeId, startVertexId, endVertexId, hops, dagId) ")
                .append("select id, ").append(id).append("L, ").append(id).append("L, startVertexId, ")
                .append(":endVertexId, (hops + 1), :dagId ")
                .append("from ").append(getDagEdgeEntityName()).append(" where endVertexId = :startVertexId");
        log.debug(insertSQL1.toString());
        Query query = entityManager.createQuery(insertSQL1.toString());
        query.setParameter("startVertexId", startVertexId);
        query.setParameter("endVertexId", endVertexId);
        query.setParameter("dagId", edge.getDagId());
        query.executeUpdate();

        //step 2: A to B's outgoing edges
        StringBuilder insertSQL2 = new StringBuilder();
        insertSQL2.append("insert into ").append(getDagEdgeEntityName()).append("(entryEdgeId, directEdgeId, exitEdgeId, startVertexId, endVertexId, hops, dagId) ")
            .append("select ").append(id).append("L, ").append(id).append("L, id, :startVertexId, ")
            .append("endVertexId, (hops + 1), :dagId ")
                .append("from ").append(getDagEdgeEntityName()).append(" where startVertexId = :endVertexId");
        log.debug(insertSQL2.toString());
        query = entityManager.createQuery(insertSQL2.toString());
        query.setParameter("startVertexId", startVertexId);
        query.setParameter("endVertexId", endVertexId);
        query.setParameter("dagId", edge.getDagId());
        query.executeUpdate();

        // step 3: A’s incoming edges to end vertex of B's outgoing edges
        StringBuilder insertSQL3 = new StringBuilder();
        insertSQL3.append("insert into ").append(getDagEdgeEntityName()).append("(entryEdgeId, directEdgeId, exitEdgeId, startVertexId, endVertexId, hops, dagId) ")
            .append("select edgeA.id, ").append(id).append("L, edgeB.id, edgeA.startVertexId, edgeB.endVertexId, (edgeA.hops + edgeB.hops + 1), :dagId ")
            .append("from ").append(getDagEdgeEntityName()).append(" edgeA, ").append(getDagEdgeEntityName()).append(" edgeB ")
            .append("where edgeA.endVertexId = :startVertexId ")
            .append("and edgeB.startVertexId = :endVertexId");
        log.debug(insertSQL3.toString());
        query = entityManager.createQuery(insertSQL3.toString());
        query.setParameter("startVertexId", startVertexId);
        query.setParameter("endVertexId", endVertexId);
        query.setParameter("dagId", edge.getDagId());
        query.executeUpdate();
    }

    @Override
    public void removeEdges(VertexID startVertexId, VertexID endVertexId) {
        Preconditions.checkArgument(startVertexId != null, "Argument [startVertexId] can not be null.");
        Preconditions.checkArgument(endVertexId != null, "Argument [endVertexId] can not be null.");
        log.debug(String.format("remove edge: from %s to %s ", startVertexId, endVertexId));

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("startVertexId", Operator.EQ, startVertexId));
        params.addPredicate(new Predicate("endVertexId", Operator.EQ, endVertexId));
        params.addPredicate(new Predicate("hops", Operator.EQ, 0));
        List<DagEdgeType> edges = find(params);
        if (edges.isEmpty()) {
            throw new IllegalArgumentException(String.format("edge from %s to %s does not exist", startVertexId, endVertexId));
        }
        else {
            DagEdge edge = edges.get(0);
            removeEdges(edge);
        }
    }

    @Override
    public void removeEdgesOfVertex(VertexID vertexId) {
        Preconditions.checkArgument(vertexId != null, "Argument [vertex] can not be null.");
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("startVertexId", Operator.EQ, vertexId));
        params.addPredicate(new Predicate("endVertexId", Operator.EQ, vertexId));
        params.setPredicatesDisjunction(true);
        List<DagEdgeType> edges = find(params);
        edges.forEach(edge -> removeEdges(edge));
    }

    @Override
    public Set<VertexID> findIncomingVertices(VertexID vertexId) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("endVertexId", Operator.EQ, vertexId));
        List<DagEdgeType> edges = find(params);
        Set<VertexID> incomingVertexIds = edges.stream().map(DagEdge::getStartVertexId).collect(Collectors.toSet());
        log.debug(String.format("incoming vertices of %s: %s", vertexId, incomingVertexIds));
        return incomingVertexIds;
    }

    @Override
    public Set<VertexID> findOutgoingVertices(VertexID vertexId) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("startVertexId", Operator.EQ, vertexId));
        List<DagEdgeType> edges = find(params);
        Set<VertexID> outgoingVertices = edges.stream().map(DagEdge::getEndVertexId).collect(Collectors.toSet());
        log.debug(String.format("outgoing vertices of %s: %s", vertexId, outgoingVertices));
        return outgoingVertices;
    }

    protected DagEdge newDagEdge() {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDagEdgeEntityName() {
        return clazz.getSimpleName();
    }

    private void removeEdges(DagEdge edge) {
        log.debug("remove edge : " + edge.getId());
        //step 1: rows that were originally inserted with the first AddEdge call for this direct edge
        Query query = entityManager.createQuery("from " + getDagEdgeEntityName() + " where directEdgeId = :id");
        query.setParameter("id", edge.getId());
        List<DagEdge> directEdges = query.getResultList();

        Set<Long> removeEdgeIds = directEdges.stream().map(DagEdge::getId).collect(Collectors.toSet());
        removeEdgeIds.add(edge.getId());
        log.debug("removeEdgeIds = " + removeEdgeIds);

        //step 2: scan and find all dependent rows that are inserted afterwards
        StringBuilder sb = new StringBuilder();
        sb.append("select id from ").append(getDagEdgeEntityName()).append(" where hops > 0 ")
                .append("and (entryEdgeId in :ids or exitEdgeId in :ids) ")
                .append("and id not in :ids");
        while (true) {
            query = entityManager.createQuery(sb.toString());
            query.setParameter("ids", removeEdgeIds);
            List<Long> ids = query.getResultList();
            log.debug(ids);
            if (ids.isEmpty()) {
                break;
            }
            removeEdgeIds.addAll(ids);
        }
        log.debug("removeEdgeIds = " + removeEdgeIds);
        batchDeleteById(removeEdgeIds);
    }
}