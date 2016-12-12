/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.model;

import java.io.Serializable;


public interface DagEdge<VertexID extends Serializable> extends Identifiable<Long>{

    public Long getId();

    public void setId(Long id);

    public Long getEntryEdgeId();

    public void setEntryEdgeId(Long entryEdgeId);

    public Long getDirectEdgeId();

    public void setDirectEdgeId(Long directEdgeId);

    public Long getExitEdgeId();

    public void setExitEdgeId(Long exitEdgeId);

    public VertexID getStartVertexId();

    public void setStartVertexId(VertexID startVertexId);

    public VertexID getEndVertexId();

    public void setEndVertexId(VertexID endVertexId);

    public Integer getHops();

    public void setHops(Integer hops);

    public String getDagId();

    public void setDagId(String dagId);
}