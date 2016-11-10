/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.util.converter;

/**
 * 
 * @author flyman
 */
public class ConversionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConversionException(String message, Throwable e) {
        super(message, e);
    }
}
