/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */
package com.limado.collab.dao;


import com.limado.collab.model.dag.DagEdge;
import com.limado.collab.model.dag.DagVertex;
import com.limado.collab.util.query.QueryParams;

import java.io.Serializable;
import java.util.List;

/**
 * Reference : http://www.codeproject.com/Articles/22824/A-Model-to-Represent-Directed-Acyclic-Graphs-DAG-o
 * 
 * @author Finion Chen
 */
public interface DagEdgeDao<VertexID extends Serializable>{
    
    public enum EDGE_DIRECTION { INCOMING, OUTGOING, BOTH };

    boolean hasEdge(VertexID startVertex, VertexID endVertex);

    DagEdge addEdge(VertexID startVertex, VertexID endVertex);

    void removeEdge(VertexID startVertex, VertexID endVertex);

    void removeVertex(VertexID vertex);

    List<DagVertex> findVertices(QueryParams params, EDGE_DIRECTION edgeDir);
    
}
