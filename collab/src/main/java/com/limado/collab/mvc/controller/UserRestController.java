/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.controller;

import com.limado.collab.model.User;
import com.limado.collab.mvc.exception.BadRequestException;
import com.limado.collab.mvc.form.UserForm;
import com.limado.collab.mvc.validator.PartyFormValidator;
import com.limado.collab.mvc.validator.ValidationUtils;
import com.limado.collab.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

/**
 * author flyman
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private static final Logger log = LogManager.getLogger(UserRestController.class);

    @Autowired
    private UserService userService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity create(@RequestBody @Validated UserForm form, BindingResult result) {
        log.debug("create userForm: " + form);
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid user data.", null, ValidationUtils.buildErrorMessage(result));
        }
        User user = form.buildModel();
        user = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping(value = "{uuidString}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void update(@PathVariable String uuidString, @RequestBody @Validated UserForm form, BindingResult result) {
        log.debug("update userForm: " + form);
        if(!form.getId().toString().equals(uuidString)) {
            throw new BadRequestException("invalid uuid.", null, String.format("path uuid %s isn't the same as uuid %s in request body", uuidString, form.getId()));
        }
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid user data.", null, ValidationUtils.buildErrorMessage(result));
        }
        User user = form.buildModel();
        userService.update(user);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(new PartyFormValidator());
    }
}
