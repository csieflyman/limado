/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.exception;

/**
 * @author csieflyman
 */
public class ResourceNotFoundException extends ServiceException {

    private static final String ERROR_CODE = "404";

    private Object resourceIdentifier;

    public ResourceNotFoundException(String message, Object resourceIdentifier)
    {
        this(message, null, resourceIdentifier);
    }

    public ResourceNotFoundException(String message, Throwable cause, Object resourceIdentifier)
    {
        super(ERROR_CODE, message, cause);
        this.resourceIdentifier = resourceIdentifier;
    }

    public Object getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public ErrorResponse getErrorResponse() {
        return new ErrorResponse(ERROR_CODE, getMessage(), resourceIdentifier != null ? resourceIdentifier.toString() : "");
    }
}
