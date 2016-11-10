/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.exception;

/**
 * @author csieflyman
 */
public class ServiceException extends RuntimeException {

    private String errorCode;

    public ServiceException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public ServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public ErrorResponse getErrorResponse() {
        return new ErrorResponse(errorCode, getMessage());
    }
}
