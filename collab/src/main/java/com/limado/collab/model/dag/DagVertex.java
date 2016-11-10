/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */
package com.limado.collab.model.dag;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * @author Finion Chen
 *
 */
public class DagVertex<ID extends Serializable>{

    private ID id;
    
    public ID getId() {
        return id;
    }
    
    public void setId(ID id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DagVertex vertex = (DagVertex) o;
        return this.getId().equals(vertex.getId());
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
