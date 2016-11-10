/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.exception;

/**
 * @author csieflyman
 */
public class BadRequestException extends ServiceException {

    private static final String ERROR_CODE = "400";

    private Object requestData;

    public BadRequestException(String message) {
        this(message, null, null);
    }

    public BadRequestException(String message, Object requestData) {
        this(message, null, requestData);
    }

    public BadRequestException(String message, Throwable cause, Object requestData) {
        super(ERROR_CODE, message, cause);
        this.requestData = requestData;
    }

    public Object getRequestData() {
        return requestData;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        return new ErrorResponse(ERROR_CODE, getMessage(), requestData != null ? requestData.toString() : "");
    }
}
