/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.model;

import java.io.Serializable;


public interface DagEdge<VertexID extends Serializable> extends Identifiable<Long>{

    Long getEntryEdgeId();

    void setEntryEdgeId(Long entryEdgeId);

    Long getDirectEdgeId();

    void setDirectEdgeId(Long directEdgeId);

    Long getExitEdgeId();

    void setExitEdgeId(Long exitEdgeId);

    VertexID getStartVertexId();

    void setStartVertexId(VertexID startVertexId);

    VertexID getEndVertexId();

    void setEndVertexId(VertexID endVertexId);

    Integer getHops();

    void setHops(Integer hops);

    String getDagId();

    void setDagId(String dagId);
}