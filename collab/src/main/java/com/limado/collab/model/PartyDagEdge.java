/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.UUID;

/**
 * @author csieflyman
 */
@Entity
@Table(name="dag_edge")
public class PartyDagEdge implements DagEdge<UUID>{

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    /**
     The ID of the incoming edge to the start vertex
     that is the creation reason for this implied edge;
     direct edges contain the same value as the Id column
     */
    @Column(name = "entry_edge_id")
    private Long entryEdgeId;

    /**
     The ID of the direct edge that caused the creation of this implied edge;
     direct edges contain the same value as the Id column
     */
    @Column(name = "direct_edge_id")
    private Long directEdgeId;

    /**
     The ID of the outgoing edge from the end vertex
     that is the creation reason for this implied edge;
     direct edges contain the same value as the Id column
     */
    @Column(name = "exit_edge_id")
    private Long exitEdgeId;

    @Basic(optional = false)
    @Column(name = "start_vertex_id")
    private UUID startVertexId;

    @Basic(optional = false)
    @Column(name = "end_vertex_id")
    private UUID endVertexId;

    /**
     Indicates how many vertex hops are necessary for the path.
     It is zero for direct edges.
     */
    @Basic(optional = false)
    @Column(name = "hops")
    private Integer hops = 0;

    /**
     A column to indicate the context in which the graph is created; useful if we have more than one DAG to be represented within the same application
     CAUTION: you need to make sure that the IDs of vertices from different sources never clash; the best is probably use of UUIDs
     */
    @Column(name = "dag_id")
    private String dagId = "party";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEntryEdgeId() {
        return entryEdgeId;
    }

    public void setEntryEdgeId(Long entryEdgeId) {
        this.entryEdgeId = entryEdgeId;
    }

    public Long getDirectEdgeId() {
        return directEdgeId;
    }

    public void setDirectEdgeId(Long directEdgeId) {
        this.directEdgeId = directEdgeId;
    }

    public Long getExitEdgeId() {
        return exitEdgeId;
    }

    public void setExitEdgeId(Long exitEdgeId) {
        this.exitEdgeId = exitEdgeId;
    }

    public UUID getStartVertexId() {
        return startVertexId;
    }

    public void setStartVertexId(UUID startVertexId) {
        this.startVertexId = startVertexId;
    }

    public UUID getEndVertexId() {
        return endVertexId;
    }

    public void setEndVertexId(UUID endVertexId) {
        this.endVertexId = endVertexId;
    }

    public Integer getHops() {
        return hops;
    }

    public void setHops(Integer hops) {
        this.hops = hops;
    }

    public String getDagId() {
        return dagId;
    }

    public void setDagId(String dagId) {
        this.dagId = dagId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DagEdge edge = (DagEdge) o;
        return this.getId().equals(edge.getId());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
