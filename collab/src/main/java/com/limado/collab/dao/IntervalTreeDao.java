package com.limado.collab.dao;

import java.io.Serializable;
import java.util.List;

/**
 * @author csieflyman
 */
public interface IntervalTreeDao<NodeIdType extends Serializable> {

    void addChild(NodeIdType parentNodeId, NodeIdType childNodeId);

    void removeChild(NodeIdType parentNodeId, NodeIdType childNodeId);

    void move(NodeIdType newParentNodeId, NodeIdType childNodeId);

    void delete(NodeIdType nodeId);

    List<NodeIdType> getSubTree(NodeIdType nodeId);
}
