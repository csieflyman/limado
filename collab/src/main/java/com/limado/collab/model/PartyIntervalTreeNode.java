/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.UUID;

/**
 * @author csieflyman
 */
@Entity
@Table(name="interval_tree")
public class PartyIntervalTreeNode implements IntervalTreeNode<UUID>{

    public static final String TREE_TYPE = "member";

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Basic(optional = false)
    @Column(name = "node_id")
    private UUID nodeId;

    @Basic(optional = false)
    @Column(name = "low")
    private Integer low;

    @Basic(optional = false)
    @Column(name = "high")
    private Integer high;

    @Basic(optional = false)
    @Column(name = "tree_id")
    private String treeId;

    @Basic(optional = false)
    @Column(name = "tree_type")
    private String treeType = TREE_TYPE;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public UUID getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getLow() {
        return low;
    }

    public void setLow(Integer low) {
        this.low = low;
    }

    public Integer getHigh() {
        return high;
    }

    public void setHigh(Integer high) {
        this.high = high;
    }

    @Override
    public String getTreeId() {
        return treeId;
    }

    @Override
    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    @Override
    public String getTreeType() {
        return treeType;
    }

    @Override
    public void setTreeType(String treeType) {
        this.treeType = treeType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(treeId).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntervalTreeNode node = (IntervalTreeNode) o;
        return new EqualsBuilder().append(this.getNodeId(), node.getNodeId()).append(this.getTreeType(), node.getTreeType()).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
