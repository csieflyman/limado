/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.query;

/**
 * @author csieflyman
 */
public enum Operator {
    EQ, NOT_EQ, GT, GE, LT, LE, IN, LIKE;

    public static String getExpr(Operator operator) {
        switch (operator) {
            case EQ:
                return "=";
            case NOT_EQ:
                return "!=";
            case GT:
                return ">";
            case GE:
                return ">=";
            case LT:
                return "<";
            case LE:
                return "<=";
            case IN:
                return "in";
            case LIKE:
                return "like";
            default:
                throw new UnsupportedOperationException(operator.name() + " is not supported");
        }
    }

    public static Operator exprValueOf(String expr) {
        switch (expr) {
            case "=":
                return EQ;
            case "!=":
                return NOT_EQ;
            case ">":
                return GT;
            case ">=":
                return GE;
            case "<":
                return LT;
            case "<=":
                return LE;
            case "in":
                return IN;
            case "IN":
                return IN;
            case "like":
                return LIKE;
            case "LIKE":
                return LIKE;
            default:
                throw new IllegalArgumentException("invalid operator expression " + expr);
        }
    }
}
