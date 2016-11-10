/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.interceptor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author csieflyman
 */
public class LoggingInterceptor extends HandlerInterceptorAdapter {

    private static final Logger log = LogManager.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("========== " + request.getMethod() + " ( " + request.getRequestURI() + " )" + " ==========");
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        long startTime = (Long)request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long executeTime = endTime - startTime;
        log.info("========== " + request.getMethod() + " ( " + request.getRequestURI() + " )" + " " + executeTime + "ms" + " ==========");
    }
}
