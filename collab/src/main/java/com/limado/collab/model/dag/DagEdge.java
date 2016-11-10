/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */
package com.limado.collab.model.dag;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * @author Finion Chen
 *
 */
public class DagEdge<VertexID extends Serializable>{

    private Long id;
    
    private Long version;
    
    /**
     * The ID of the incoming edge to the start vertex
     * that is the creation reason for this implied edge;
     * direct edges contain the same value as the Id column
     * 
     */
    private Long entryEdgeId;
    
    /**
     * The ID of the direct edge that caused the creation of this implied edge;
     * direct edges contain the same value as the Id column
     * 
     */
    private Long directEdgeId;
    
    /**
     * The ID of the outgoing edge from the end vertex
     * that is the creation reason for this implied edge;
     * direct edges contain the same value as the Id column
     * 
     */
    private Long exitEdgeId;
    
    /**
     * Indicates how many vertex hops are necessary for the path.
     * It is zero for direct edges.
     */
    private Long hops = 0L;
    
    /**
     *  Identity of the graph.
     */
    private String dagId;
    
    private VertexID startVertex;
    
    private VertexID endVertex;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
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

    public Long getHops() {
        return hops;
    }

    public void setHops(Long hops) {
        this.hops = hops;
    }

    public String getDagId() {
        return dagId;
    }

    public void setDagId(String dagId) {
        this.dagId = dagId;
    }

    public VertexID getStartVertex() {
        return startVertex;
    }

    public void setStartVertex(VertexID startVertex) {
        this.startVertex = startVertex;
    }

    public VertexID getEndVertex() {
        return endVertex;
    }

    public void setEndVertex(VertexID endVertex) {
        this.endVertex = endVertex;
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