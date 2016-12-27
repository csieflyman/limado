package com.limado.collab.mvc.advice;

import com.limado.collab.mvc.exception.BadRequestException;
import com.limado.collab.mvc.exception.ErrorResponse;
import com.limado.collab.mvc.exception.ResourceNotFoundException;
import com.limado.collab.util.converter.ConversionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * @author csieflyman
 */
@ControllerAdvice
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LogManager.getLogger(ExceptionControllerAdvice.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("[Resource Not Found] " + request.getMethod() + " ( " + request.getRequestURI() + " )", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getErrorResponse());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.error("[Bad Request] " + request.getMethod() + " ( " + request.getRequestURI() + " )", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getErrorResponse());
    }

    //HttpMessageNotReadableException
    @ExceptionHandler(value = {IllegalArgumentException.class, UnsupportedOperationException.class, ConversionException.class})
    public ResponseEntity handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        log.error("[Bad Request] " + request.getMethod() + " ( " + request.getRequestURI() + " )", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("400", ex.getMessage()));
    }

    // Default Exception
    @ExceptionHandler(Throwable.class)
    public ResponseEntity handleDefaultException(Throwable ex, HttpServletRequest request) {
        log.error("[Internal Server Error] " + request.getMethod() + " ( " + request.getRequestURI() + " )", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("500", ex.getMessage()));
    }
}
