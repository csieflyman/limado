/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.query;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * author flyman
 */
public class QueryParamsTest {

    @Test
    public void parseOrderByString() {
        QueryParams params = new QueryParams();
        params.put(QueryParams.Q_SORT, "+aaa,-bbb,ccc");
        List<OrderBy> orderByList = params.getOrderByList();
        Assert.assertEquals(Arrays.asList(new OrderBy("aaa", true), new OrderBy("bbb", false), new OrderBy("ccc")), orderByList);
    }

    @Test
    public void parsePredicateString() {
        QueryParams params = new QueryParams();
        params.put(QueryParams.Q_PREDICATES, "[aaa = 111 ; bbb in (1,2,3)]");
        List<Predicate> predicates = params.getPredicates();
        Assert.assertEquals(Arrays.asList(new Predicate("aaa", Operator.EQ, "111"), new Predicate("bbb", Operator.IN, "(1,2,3)")), predicates);
    }

    @Test
    public void getPredicateRelations() {
        QueryParams params = new QueryParams();
        params.put(QueryParams.Q_PREDICATES, "[a.x = x ; b.y = y]");
        Set<String> predicateRelations = params.getPredicateRelations();
        Assert.assertEquals(Sets.newHashSet("a", "b"), predicateRelations);
    }
}
