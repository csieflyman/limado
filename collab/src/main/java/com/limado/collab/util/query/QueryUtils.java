/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.query;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.limado.collab.util.converter.BeanPropertyConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Query;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * author flyman
 */
public class QueryUtils {

    private static final Logger log = LogManager.getLogger(QueryUtils.class);

    private QueryUtils(){

    }

    public static String buildHqlWhereClause(EntityType rootEntityType, Collection<Predicate> predicates, boolean isOrPredicate) {
        if(predicates.isEmpty())
            return "";

        populatePredicate(rootEntityType, predicates);

        StringBuilder sb = new StringBuilder();
        for(Predicate predicate: predicates) {
            Operator operator = predicate.getOperator();
            String operatorExpr = Operator.getExpr(operator);
            if(predicate.isEntityTypePredicate()) {
                sb.append(predicate.getProperty()).append(" ").append(operatorExpr).append(" ").append(predicate.getValue());
            }
            else if(Operator.isNoValue(operator)) {
                sb.append(predicate.getProperty()).append(" ").append(operatorExpr);
            }
            else {
                String property = predicate.getProperty();
                if(!property.contains(".")) {
                    property = getEntityAlias(rootEntityType) + "." + property;
                }
                sb.append(property).append(" ").append(operatorExpr).append(" ").append(":").append(predicate.getQueryParameterName());
            }

            if(isOrPredicate)
                sb.append(" or ");
            else
                sb.append(" and ");
        }

        if(isOrPredicate) {
            sb.delete(sb.length() - 4, sb.length());
            sb.insert(0, " ( ");
            sb.append(" ) ");
        }
        else {
            sb.delete(sb.length() - 5, sb.length());
        }
        return sb.toString();
    }

    public static void setQueryParameterValue(Query query, Collection<Predicate> predicates) {
        for(Predicate predicate: predicates) {
            if(predicate.isEntityTypePredicate() || Operator.isNoValue(predicate.getOperator()))
                continue;
            query.setParameter(predicate.getQueryParameterName(), predicate.getQueryParameterValue());
        }
    }

    public static String buildHqlOrderByClause(EntityType rootEntityType, List<OrderBy> orderByList) {
        if (orderByList.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append("order by ");
        for (OrderBy orderBy: orderByList) {
            String property = orderBy.getProperty();
            if(!property.contains(".")) {
                property = getEntityAlias(rootEntityType) + "." + property;
            }
            sb.append(property).append(" ").append(orderBy.isAsc() ? "" : "DESC ").append(", ");
        }
        sb.delete(sb.length() -2, sb.length());
        return sb.toString();
    }

    public static String getEntityAlias(EntityType entityType) {
        String entityClassName = entityType.getName();
        String entityClassSimpleName = entityClassName.substring(entityClassName.lastIndexOf(".") + 1);
        return StringUtils.uncapitalize(entityClassSimpleName);
    }

    private static void populatePredicate(EntityType rootEntityType, Collection<Predicate> predicates) {
        for(Predicate predicate: predicates) {
            if(predicate.isEntityTypePredicate())
                continue;
            validatePredicateProperty(rootEntityType, predicate);
        }

        populateQueryParameterName(predicates);

        for(Predicate predicate: predicates) {
            if(predicate.isEntityTypePredicate())
                continue;
            populateQueryParameterValue(rootEntityType, predicate);
        }
        log.debug(predicates);
    }

    private static void validatePredicateProperty(EntityType rootEntityType, Predicate predicate) {
        rootEntityType.getAttribute(predicate.getTopProperty()); // validate attribute

        if(predicate.isNestedProperty()) {
            Attribute association = rootEntityType.getAttribute(predicate.getTopProperty());
            association.getDeclaringType().getAttribute(predicate.getNestedProperty()); // validate attribute
        }
    }

    private static void populateQueryParameterName(Collection<Predicate> predicates) {
        Map<String, Integer> propertyCountMap = new HashMap<>();
        for(Predicate predicate: predicates) {
            String property = predicate.getProperty();
            if (propertyCountMap.containsKey(property)) {
                propertyCountMap.put(property, (propertyCountMap.get(property) + 1));
                predicate.setQueryParameterName(property + propertyCountMap.get(property));
            } else {
                propertyCountMap.put(property, 1);
                predicate.setQueryParameterName(property);
            }
            if(predicate.isNestedProperty()) {
                predicate.setQueryParameterName(predicate.getQueryParameterName().replace(".", "_"));
            }
        }
    }

    private static void populateQueryParameterValue(EntityType rootEntityType, Predicate predicate) {
        if(Operator.isNoValue(predicate.getOperator())) {
            return;
        }

        String propertyName = predicate.getTopProperty();
        Attribute attribute = rootEntityType.getAttribute(propertyName);
        if(attribute.isAssociation()) {
            attribute = attribute.getDeclaringType().getAttribute(predicate.getNestedProperty());
        }
        propertyName = attribute.getName();
        Class queryParameterValueClass = attribute.getJavaType();
        Object queryParameterValue;
        Object value = predicate.getValue();
        Operator operator = predicate.getOperator();
        if(operator == Operator.IN) {
            if(value instanceof String) {
                String valueString = ((String)value).trim();
                if(!(valueString.startsWith("(") && valueString.endsWith(")"))) {
                    throw new IllegalArgumentException(String.format("%s value string %s should be with the format: (e1, e2)", propertyName, valueString));
                }
                valueString = valueString.substring(1, valueString.length() - 1);
                queryParameterValue = Lists.newArrayList(Splitter.on(",").split(valueString)).stream().map(element -> BeanPropertyConverter.convert(element.trim(), queryParameterValueClass)).collect(Collectors.toList());
            }
            else if(value instanceof Collection){
                if(((Collection) value).isEmpty()) {
                    throw new IllegalArgumentException(String.format("%s with in operator can't has empty collection value", propertyName));
                }
                List<Object> queryParameterValueList = new ArrayList<>();
                for(Object element: (Collection) value) {
                    if(element.getClass() != queryParameterValueClass) {
                        queryParameterValueList.add(BeanPropertyConverter.convert(element, queryParameterValueClass));
                    }
                }
                queryParameterValue = queryParameterValueList.isEmpty() ? value : queryParameterValueList;
            }
            else {
                throw new IllegalArgumentException(String.format("%s value %s should be a collection or a string with the format: (e1, e2)", propertyName, value.getClass().getName()));
            }
        }
        else {
            queryParameterValue = queryParameterValueClass == value.getClass() ? value : BeanPropertyConverter.convert(value, queryParameterValueClass);
        }
        predicate.setQueryParameterValue(queryParameterValue);
    }
}
