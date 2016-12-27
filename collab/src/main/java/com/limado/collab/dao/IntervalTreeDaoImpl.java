package com.limado.collab.dao;

import com.google.common.base.Preconditions;
import com.limado.collab.model.IntervalTreeNode;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.OrderBy;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Query;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author csieflyman
 */
abstract class IntervalTreeDaoImpl<NodeType extends IntervalTreeNode<NodeIdType>, NodeIdType extends Serializable>
        extends JpaGenericDaoImpl<NodeType, Long> implements IntervalTreeDao<NodeIdType> {

    private static final Logger log = LogManager.getLogger(IntervalTreeDaoImpl.class);

    abstract protected String getTreeType();

    @Override
    public void addChild(NodeIdType parentNodeId, NodeIdType childNodeId) {
        Preconditions.checkArgument(parentNodeId != null, "parentNodeId must not be null");
        Preconditions.checkArgument(childNodeId != null, "childNodeId must not be null");

        NodeType parentNode = getNode(parentNodeId);
        if (parentNode == null) {
            parentNode = create(newNode(parentNodeId));
        }
        NodeType childNode = getNode(childNodeId);
        if (childNode == null) {
            childNode = create(newNode(childNodeId));
        }
        addChild(parentNode, childNode);
    }

    @Override
    public void removeChild(NodeIdType parentNodeId, NodeIdType childNodeId) {
        Preconditions.checkArgument(parentNodeId != null, "parentNodeId must not be null");
        Preconditions.checkArgument(childNodeId != null, "childNodeId must not be null");

        NodeType parentNode = getNode(parentNodeId);
        NodeType childNode = getNode(childNodeId);
        removeChild(parentNode, childNode, true);
    }

    @Override
    public void move(NodeIdType newParentNodeId, NodeIdType childNodeId) {
        Preconditions.checkArgument(newParentNodeId != null, "newParentNodeId must not be null");
        Preconditions.checkArgument(childNodeId != null, "childNodeId must not be null");

        NodeType childNode = getNode(childNodeId);
        NodeType currentParentNode = getParent(childNode);
        if (currentParentNode != null) {
            removeChild(currentParentNode, childNode, false);
            childNode = getNode(childNodeId);
        }
        NodeType newParentNode = getNode(newParentNodeId);
        addChild(newParentNode, childNode);
    }

    @Override
    public void delete(NodeIdType nodeId) {
        Preconditions.checkArgument(nodeId != null, "nodeId must not be null");

        NodeType node = getNode(nodeId);
        if (node == null)
            return;

        delete(node);
    }

    @Override
    public void delete(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        NodeType parentNode = getParent(node);
        if (parentNode != null) {
            removeChild(parentNode, node, true);
            node = getNode(node.getNodeId());
        }
        if (node != null && !isLeaf(node)) {
            List<NodeType> children = getChildren(node);
            for (NodeType child : children) {
                node = getNode(node.getNodeId());
                child = getNode(child.getNodeId());
                removeChild(node, child, true);
            }
        }
    }

    @Override
    public List<NodeIdType> getSubTree(NodeIdType nodeId) {
        Preconditions.checkArgument(nodeId != null, "nodeId must not be null");

        NodeType node = getNode(nodeId);
        if (node == null) {
            return Collections.emptyList();
        }
        List<NodeType> subTreeNodes = getSubTree(node);
        return subTreeNodes.stream().map(NodeType::getNodeId).collect(Collectors.toList());
    }

    private void addChild(NodeType parentNode, NodeType childNode) {
        Preconditions.checkArgument(parentNode != null, "parentNode must not be null");
        Preconditions.checkArgument(childNode != null, "childNode must not be null");

        int childTreeSize = getSize(childNode);
        updateFollowUpNodesOfParentTree(parentNode, parentNode.getHigh(), true, 2 * childTreeSize);
        updateAncestorsOfParentTree(parentNode, true, 2 * childTreeSize);
        updateChildTree(childNode, childNode.getTreeId(), parentNode.getTreeId(), true, parentNode.getHigh() - 1);
    }

    private void removeChild(NodeType parentNode, NodeType childNode, boolean isDeleteLeafChild) {
        Preconditions.checkArgument(parentNode != null, "parentNode must not be null");
        Preconditions.checkArgument(childNode != null, "childNode must not be null");

        if (isDeleteLeafChild && isLeaf(childNode)) {
            NodeType childRef = entityManager.getReference(clazz, childNode.getId());
            super.delete(childRef);
            entityManager.flush();
            entityManager.detach(childNode);
        } else {
            updateChildTree(childNode, childNode.getTreeId(), childNode.getNodeId().toString(), false, childNode.getLow() - 1);
        }

        int childTreeSize = getSize(childNode);
        updateFollowUpNodesOfParentTree(parentNode, childNode.getHigh(), false, 2 * childTreeSize);
        updateAncestorsOfParentTree(parentNode, false, 2 * childTreeSize);

        parentNode = getNode(parentNode.getNodeId());
        if (isRootWithoutChild(parentNode)) {
            super.delete(parentNode);
            entityManager.flush();
            entityManager.detach(parentNode);
        }
    }

    private void updateFollowUpNodesOfParentTree(NodeType parentNode, int start, boolean incrementOffset, int offset) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("low", Operator.GT, start));
        params.addPredicate(new Predicate("treeId", Operator.EQ, parentNode.getTreeId()));
        params.addPredicate(new Predicate("treeType", Operator.EQ, getTreeType()));
        List<NodeType> nodes = find(params);
        Set<Long> ids = nodes.stream().map(NodeType::getId).collect(Collectors.toSet());
        log.debug(ids);
        if (ids.isEmpty())
            return;

        String operator = incrementOffset ? "+" : "-";
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(getEntityName()).append(" set low = low ").append(operator).append(" :offset, ")
                .append("high = high ").append(operator).append(" :offset ")
                .append("where id in :ids");
        Query query = entityManager.createQuery(sb.toString());
        query.setParameter("offset", offset);
        query.setParameter("ids", ids);
        query.executeUpdate();

        entityManager.flush();
        nodes.forEach(node -> entityManager.detach(node));
    }

    private void updateAncestorsOfParentTree(NodeType parentNode, boolean incrementOffset, int offset) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("low", Operator.LE, parentNode.getLow()));
        params.addPredicate(new Predicate("high", Operator.GE, parentNode.getHigh()));
        params.addPredicate(new Predicate("treeId", Operator.EQ, parentNode.getTreeId()));
        params.addPredicate(new Predicate("treeType", Operator.EQ, getTreeType()));
        List<NodeType> nodes = find(params);
        Set<Long> ids = nodes.stream().map(NodeType::getId).collect(Collectors.toSet());
        log.debug(ids);
        if (ids.isEmpty())
            return;

        String operator = incrementOffset ? "+" : "-";
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(getEntityName()).append(" set high = high ").append(operator).append(" :offset ")
                .append("where id in :ids");
        Query query = entityManager.createQuery(sb.toString());
        query.setParameter("offset", offset);
        query.setParameter("ids", ids);
        query.executeUpdate();

        entityManager.flush();
        nodes.forEach(node -> entityManager.detach(node));
    }

    private void updateChildTree(NodeType childNode, String oldTreeId, String newTreeId, boolean incrementOffset, int offset) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("low", Operator.GE, childNode.getLow()));
        params.addPredicate(new Predicate("high", Operator.LE, childNode.getHigh()));
        params.addPredicate(new Predicate("treeId", Operator.EQ, oldTreeId));
        params.addPredicate(new Predicate("treeType", Operator.EQ, getTreeType()));
        List<NodeType> nodes = find(params);
        Set<Long> ids = nodes.stream().map(NodeType::getId).collect(Collectors.toSet());
        log.debug(ids);
        if (ids.isEmpty())
            return;

        String operator = incrementOffset ? "+" : "-";
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(getEntityName()).append(" set low = low ").append(operator).append(" :offset, ")
                .append("high = high ").append(operator).append(" :offset, treeId = :newTreeId ")
                .append("where id in :ids");
        Query query = entityManager.createQuery(sb.toString());
        query.setParameter("offset", offset);
        query.setParameter("newTreeId", newTreeId);
        query.setParameter("ids", ids);
        query.executeUpdate();

        entityManager.flush();
        nodes.forEach(node -> entityManager.detach(node));
    }

    private List<NodeType> getSubTree(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        if (isLeaf(node))
            return Collections.emptyList();

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("low", Operator.GT, node.getLow()));
        params.addPredicate(new Predicate("high", Operator.LT, node.getHigh()));
        params.addPredicate(new Predicate("treeId", Operator.EQ, node.getTreeId()));
        params.addPredicate(new Predicate("treeType", Operator.EQ, getTreeType()));
        params.addOrderBy(new OrderBy("low"));
        return find(params);
    }

    private NodeType getNode(NodeIdType nodeId) {
        Preconditions.checkArgument(nodeId != null, "nodeId must not be null");

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("nodeId", Operator.EQ, nodeId));
        params.addPredicate(new Predicate("treeType", Operator.EQ, getTreeType()));
        List<NodeType> result = find(params);
        return result.isEmpty() ? null : result.get(0);
    }

    private NodeType getParent(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        if (isRoot(node))
            return null;

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("low", Operator.LT, node.getLow()));
        params.addPredicate(new Predicate("high", Operator.GT, node.getHigh()));
        params.addPredicate(new Predicate("treeId", Operator.EQ, node.getTreeId()));
        params.addPredicate(new Predicate("treeType", Operator.EQ, getTreeType()));
        List<NodeType> ancestors = find(params);
        return ancestors.stream().min(Comparator.comparing(ancestor -> node.getLow() - ancestor.getLow())).get();
    }

    private List<NodeType> getChildren(NodeType node) {
        List<NodeType> subTreeNodes = getSubTree(node);
        if (subTreeNodes.isEmpty())
            return Collections.emptyList();

        List<NodeType> children = new ArrayList<>();
        do {
            NodeType firstChild = subTreeNodes.get(0);
            children.add(firstChild);
            subTreeNodes.remove(firstChild);
            subTreeNodes.removeAll(getSubTree(firstChild));
        } while (!subTreeNodes.isEmpty());
        return children;
    }

    private int getSubTreeSize(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        return (node.getHigh() - 1 - node.getLow()) / 2;
    }

    private int getSize(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        return 1 + getSubTreeSize(node);
    }

    private boolean isRoot(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        return node.getLow() == 1;
    }

    private boolean isRootWithoutChild(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        return node.getLow() == 1 && node.getHigh() == 2;
    }

    private boolean isLeaf(NodeType node) {
        Preconditions.checkArgument(node != null, "node must not be null");

        return (node.getHigh() - node.getLow()) == 1;
    }

    protected NodeType newNode(NodeIdType nodeId) {
        NodeType node = super.newInstance();
        node.setNodeId(nodeId);
        node.setLow(1);
        node.setHigh(2);
        node.setTreeId(nodeId.toString());
        node.setTreeType(getTreeType());
        return node;
    }
}
