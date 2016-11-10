/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.exception;

/**
 * @author csieflyman
 */
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String detail = "";

    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public ErrorResponse(String errorCode, String message, String detail) {
        this.errorCode = errorCode;
        this.message = message;
        this.detail = detail;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
