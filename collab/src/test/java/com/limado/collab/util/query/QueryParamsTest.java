package com.limado.collab.util.query;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * author flyman
 */
public class QueryParamsTest {

    private static final Logger log = LogManager.getLogger(QueryParamsTest.class);

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
        params.put(QueryParams.Q_PREDICATES, "[aaa = a b c  ; bbb in (1, 2, 3)]");
        List<Predicate> predicates = params.getPredicates();
        Assert.assertEquals(Arrays.asList(new Predicate("aaa", Operator.EQ, "a b c"), new Predicate("bbb", Operator.IN, "(1, 2, 3)")), predicates);
    }

    @Test
    public void getPredicateRelations() {
        QueryParams params = new QueryParams();
        params.put(QueryParams.Q_PREDICATES, "[TYPE(entity) in (x,y); a.x = x ; b.y = y]");
        Set<String> predicateRelations = params.getPredicateProperties();
        Assert.assertEquals(Sets.newHashSet("a", "b"), predicateRelations);
    }
}
