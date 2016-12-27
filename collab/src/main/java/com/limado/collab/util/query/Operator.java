package com.limado.collab.util.query;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

/**
 * @author csieflyman
 */
public enum Operator {
    EQ, NOT_EQ, GT, GE, LT, LE, IN, LIKE, IS_NULL, IS_NOT_NULL, IS_EMPTY, IS_NOT_EMPTY;

    private static final BidiMap OPERATOR_EXPR_MAP = new DualHashBidiMap();

    static {
        OPERATOR_EXPR_MAP.put(EQ, "=");
        OPERATOR_EXPR_MAP.put(NOT_EQ, "!=");
        OPERATOR_EXPR_MAP.put(GT, ">");
        OPERATOR_EXPR_MAP.put(GE, ">=");
        OPERATOR_EXPR_MAP.put(LT, "<");
        OPERATOR_EXPR_MAP.put(LE, "<=");
        OPERATOR_EXPR_MAP.put(IN, "in");
        OPERATOR_EXPR_MAP.put(LIKE, "like");
        OPERATOR_EXPR_MAP.put(IS_NULL, "is_null");
        OPERATOR_EXPR_MAP.put(IS_NOT_NULL, "is_not_null");
        OPERATOR_EXPR_MAP.put(IS_EMPTY, "is_empty");
        OPERATOR_EXPR_MAP.put(IS_NOT_EMPTY, "is_not_empty");
    }

    public static boolean isNoValue(Operator operator) {
        return operator == IS_NULL || operator == IS_NOT_NULL || operator == IS_EMPTY || operator == IS_NOT_EMPTY;
    }

    public static String getExpr(Operator operator) {
        String expr = (String) OPERATOR_EXPR_MAP.get(operator);
        expr = expr.replaceAll("_", " ");
        return expr;
    }

    public static Operator exprValueOf(String expr) {
        Operator operator = (Operator) OPERATOR_EXPR_MAP.getKey(expr);
        if (operator == null) {
            throw new IllegalArgumentException("invalid operator expression " + expr);
        }
        return operator;
    }
}
