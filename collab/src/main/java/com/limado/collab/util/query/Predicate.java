package com.limado.collab.util.query;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Collection;

/**
 * author flyman
 */
public class Predicate {

    private static final String ENTITY_TYPE_EXPR_PATTERN = "TYPE\\(\\S*\\)";

    private String property;

    private Operator operator;

    private Object value;

    private String queryParameterName;

    private Object queryParameterValue;

    public Predicate(String property, Operator operator, Object value) {
        Preconditions.checkArgument(property != null, "property is null");
        Preconditions.checkArgument(operator != null, "operator is null");
        Preconditions.checkArgument(property.split("\\.").length <= 2, "nested relation or component is not supported now! " + property);
        if (!Operator.isNoValue(operator)) {
            Preconditions.checkArgument(value != null, "value is null");
        }
        if (operator == Operator.IN && value instanceof Collection) {
            Preconditions.checkArgument(!((Collection) value).isEmpty(), property + " with in operator can't has empty collection value");
        }

        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    public boolean isEntityTypePredicate() {
        return property.matches(ENTITY_TYPE_EXPR_PATTERN);
    }

    public String getProperty() {
        return property;
    }

    public Operator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public String getQueryParameterName() {
        return queryParameterName;
    }

    public void setQueryParameterName(String queryParameterName) {
        Preconditions.checkNotNull(queryParameterName, "null value of predicate queryParameterName " + this);
        this.queryParameterName = queryParameterName;
    }

    public Object getQueryParameterValue() {
        return queryParameterValue;
    }

    public void setQueryParameterValue(Object queryParameterValue) {
        Preconditions.checkNotNull(queryParameterValue, "null value of predicate queryParameterValue " + this);
        this.queryParameterValue = queryParameterValue;
    }

    public boolean isNestedProperty() {
        return property.contains(".");
    }

    public String getNestedProperty() {
        return property.split("\\.")[1];
    }

    public String getTopProperty() {
        return property.split("\\.")[0];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Predicate predicate = (Predicate) o;
        return new EqualsBuilder().append(this.getProperty(), predicate.getProperty())
                .append(this.getOperator(), predicate.getOperator()).append(this.getValue(), predicate.getValue()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getProperty()).append(this.getOperator()).append(this.getValue()).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
