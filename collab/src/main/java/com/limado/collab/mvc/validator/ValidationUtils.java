package com.limado.collab.mvc.validator;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * @author csieflyman
 */
public class ValidationUtils {

    private ValidationUtils() {

    }

    public static String buildErrorMessage(BindingResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.hasErrors()) {
            for (FieldError error : result.getFieldErrors()) {
                sb.append(error.getField() + " : " + error.getDefaultMessage()).append("; ");
            }
            if (sb.length() > 0) {
                sb = sb.delete(sb.length() - 2, sb.length());
            }
        }
        return sb.toString();
    }
}
