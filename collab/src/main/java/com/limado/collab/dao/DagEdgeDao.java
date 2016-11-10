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
    
    /**
     * Check the existence of edge in the DAG
     * 
     * @param startVertex
     * @param endVertex
     * @return
     */
    boolean hasEdge(VertexID startVertex, VertexID endVertex);
   
    /**
     * Add edge to the DAG.
     * 
     * @param startVertex
     * @param endVertex
     * @return
     */
    DagEdge addEdge(VertexID startVertex, VertexID endVertex);
    
    /**
     * Remove edge from DAG.
     * 
     * @param startVertex
     * @param endVertex
     */
    void removeEdge(VertexID startVertex, VertexID endVertex);
    
    /**
     * Remove vertex from DAG.
     * 
     * @param vertex
     */
    void removeVertex(VertexID vertex);
    
    /**
     * Finding the vertices
     * 
     * @param params
     * @param edgeDir the direction of vertices expect to return as results
     * @return
     */
    List<DagVertex> findVertices(QueryParams params, EDGE_DIRECTION edgeDir);
    
}
