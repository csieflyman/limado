/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.util.Arrays;

/**
 * @author csieflyman
 */
public class DebugLoggingResultHandler implements ResultHandler {

    private static final Logger log = LogManager.getLogger(DebugLoggingResultHandler.class);

    @Override
    public void handle(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        MockHttpServletRequest request = result.getRequest();
        log.debug("request parameter:");
        request.getParameterMap().forEach((key, value) -> log.debug(key + " = " + Arrays.asList(value)));
        log.debug("response status = " + response.getStatus());
        log.debug("response body: " + response.getContentAsString());
        log.debug("exception: ", result.getResolvedException());
    }
}
