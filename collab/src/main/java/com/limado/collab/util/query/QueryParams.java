/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.query;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * author flyman
 */
public class QueryParams {

    private static final Logger log = LogManager.getLogger(QueryParams.class);

    private int offset = -1;
    private int limit = 10;
    private boolean onlySize = false;
    private List<OrderBy> orderByList = new ArrayList<>();
    private boolean predicatesDisjunction = false;
    private List<Predicate> predicates = new ArrayList<>();
    private Set<String> fetchRelations = new HashSet<>();
    private Set<String> fetchProperties = new HashSet<>();

    public static final String Q_OFFSET = "q_offset";
    public static final String Q_LIMIT = "q_limit";
    public static final String Q_SORT = "q_sort";
    public static final String Q_ONLY_SIZE = "q_onlySize";
    public static final String Q_PREDICATES = "q_predicates";
    public static final String Q_PREDICATES_DISJUNCTION = "q_predicatesDisjunction";
    public static final String Q_FETCH_RELATIONS = "q_fetchRelations";
    public static final String Q_FETCH_PROPERTIES = "q_fetchProperties";

    public void put(Map<String, String> paramMap) {
        paramMap.forEach((key, value) -> put(key, value));
    }

    public void put(String param, String value) {
        Preconditions.checkArgument(value != null, "value must not be null");
        String valueString = String.valueOf(value);
        switch (param) {
            case Q_OFFSET:
                setOffset(Integer.parseInt(valueString));
                break;
            case Q_LIMIT:
                setLimit(Integer.parseInt(valueString));
                break;
            case Q_SORT:
                setOrderByList(parseSortString(valueString));
                break;
            case Q_ONLY_SIZE:
                setOnlySize(Boolean.parseBoolean(valueString));
                break;
            case Q_PREDICATES:
                setPredicates(parsePredicateString(valueString));
                break;
            case Q_PREDICATES_DISJUNCTION:
                setPredicatesDisjunction(Boolean.parseBoolean(valueString));
                break;
            case Q_FETCH_RELATIONS:
                setFetchRelations(Sets.newHashSet(Splitter.on(",").split(valueString))
                        .stream().map(relation -> relation.trim()).collect(Collectors.toSet()));
                break;
            case Q_FETCH_PROPERTIES:
                setFetchProperties(Sets.newHashSet(Splitter.on(",").split(valueString))
                        .stream().map(property -> property.trim()).collect(Collectors.toSet()));
                break;
            default:
                throw new IllegalArgumentException("unknown query parameter: " + param);
        }
    }

    private List<OrderBy> parseSortString(String s) {
        if(StringUtils.isEmpty(s))
            return Collections.emptyList();

        List<OrderBy> orderByList = new ArrayList<>();
        String[] orderByProps = s.split(",");
        for(String orderByProp: orderByProps) {
            orderByProp = orderByProp.trim();
            if(orderByProp.startsWith("+")) {
                orderByList.add(new OrderBy(orderByProp.substring(1), true));
            }
            else if(orderByProp.startsWith("-")){
                orderByList.add(new OrderBy(orderByProp.substring(1), false));
            }
            else {
                orderByList.add(new OrderBy(orderByProp, true));
            }
        }
        return orderByList;
    }

    private List<Predicate> parsePredicateString(String s) {
        if(StringUtils.isEmpty(s))
            return Collections.emptyList();

        Preconditions.checkArgument(s.startsWith("[") && s.endsWith("]"), "invalid q_predicate format: " + s);
        s = s.substring(1, s.length() -1).trim();
        List<String> predicateStrings = Lists.newArrayList(Splitter.on(";").split(s));
        List<Predicate> predicates;
        try {
            predicates = predicateStrings.stream().map(ps -> ps.trim()).filter(ps -> StringUtils.isNotEmpty(ps)).map(ps -> Lists.newArrayList(Splitter.on(" ").split(ps)))
                    .map(psList -> new Predicate(psList.get(0), Operator.exprValueOf(psList.get(1)),
                            psList.size() == 2 ? null : String.join(" ", psList.subList(2, psList.size())))).collect(Collectors.toList());
        }catch (Throwable e) {
            throw new IllegalArgumentException("invalid q_predicate format: " + s, e);
        }
        return predicates;
    }

    public Set<String> getPredicateProperties() {
        return predicates.stream().filter(predicate -> !predicate.isEntityTypePredicate()).map(predicate -> predicate.getProperty())
                .map(property -> property.contains(".") ? property.substring(0, property.indexOf(".")) : property).collect(Collectors.toSet());
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        Preconditions.checkArgument(offset >= 0);
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        Preconditions.checkArgument(limit > 0);
        this.limit = limit;
    }

    public boolean isOnlySize() {
        return onlySize;
    }

    public void setOnlySize(boolean onlySize) {
        this.onlySize = onlySize;
    }

    public List<OrderBy> getOrderByList() {
        return orderByList;
    }

    public void setOrderByList(List<OrderBy> orderByList) {
        this.orderByList = orderByList;
    }

    public void addOrderBy(OrderBy orderBy) {
        orderByList.add(orderBy);
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    public void addPredicate(Predicate predicate) {
        predicates.add(predicate);
    }

    public Set<String> getFetchRelations() {
        return fetchRelations;
    }

    public void setFetchRelations(Set<String> fetchRelations) {
        this.fetchRelations = fetchRelations;
    }

    public boolean isPredicatesDisjunction() {
        return predicatesDisjunction;
    }

    public Set<String> getFetchProperties() {
        return fetchProperties;
    }

    public void setFetchProperties(Set<String> fetchProperties) {
        this.fetchProperties = fetchProperties;
    }

    public void setPredicatesDisjunction(boolean predicatesDisjunction) {
        this.predicatesDisjunction = predicatesDisjunction;
}

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
