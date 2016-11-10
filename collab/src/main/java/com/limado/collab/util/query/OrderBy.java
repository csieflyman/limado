/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.query;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * author flyman
 */
public class OrderBy {

    private boolean asc = true;

    private String property;

    public OrderBy(String property, boolean asc) {
        this.asc = asc;
        this.property = property;
    }

    public OrderBy(String property) {
        this.property = property;
    }

    public boolean isAsc() {
        return asc;
    }

    public void setAsc(boolean asc) {
        this.asc = asc;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getProperty()).append(this.isAsc()).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderBy orderBy = (OrderBy) o;
        return new EqualsBuilder().append(this.getProperty(), orderBy.getProperty())
                .append(this.isAsc(), orderBy.isAsc()).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
