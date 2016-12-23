/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.controller;

import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import com.limado.collab.mvc.exception.BadRequestException;
import com.limado.collab.mvc.form.UserForm;
import com.limado.collab.mvc.validator.PartyFormValidator;
import com.limado.collab.mvc.validator.ValidationUtils;
import com.limado.collab.service.UserService;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public ResponseEntity create(@RequestBody UserForm form, BindingResult result) {
        log.debug("create userForm: " + form);
        new PartyFormValidator().validate(form, result);
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid user data.", null, ValidationUtils.buildErrorMessage(result));
        }
        User user = form.buildModel();
        user = userService.create(user);
        user.setParents(null);
        user.setChildren(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void update(@PathVariable String id, @RequestBody UserForm form, BindingResult result) {
        log.debug("update userForm: " + form);
        if(!form.getId().toString().equals(id)) {
            throw new BadRequestException("invalid uuid.", null, String.format("path uuid %s isn't the same as uuid %s in request body", id, form.getId()));
        }
        new PartyFormValidator().validate(form, result);
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid user data.", null, ValidationUtils.buildErrorMessage(result));
        }
        User user = form.buildModel();
        userService.update(user);
    }

    @PostMapping(value = "{childId}/parents", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void addParents(@PathVariable String childId, @RequestBody List<String> parentsIds) {
        if (parentsIds.isEmpty())
            return;

        UUID childUUID = UUID.fromString(childId);
        Set<UUID> parentsUUIDs = parentsIds.stream().map(UUID::fromString).collect(Collectors.toSet());
        Party user = userService.getById(childUUID);
        if(user != null && !user.getType().equals(User.TYPE)) {
            throw new BadRequestException(String.format("%s is not a user", user));
        }
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, parentsUUIDs));
        List<Party> parents = userService.find(params);
        userService.addParents((User) user, new HashSet<>(parents));
    }

    @DeleteMapping(value = "{childId}/parents", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void removeParents(@PathVariable String childId, @RequestBody List<String> parentsIds) {
        if (parentsIds.isEmpty())
            return;

        UUID childUUID = UUID.fromString(childId);
        Set<UUID> parentsUUIDs = parentsIds.stream().map(UUID::fromString).collect(Collectors.toSet());
        Party user = userService.getById(childUUID);
        if(user != null && !user.getType().equals(User.TYPE)) {
            throw new BadRequestException(String.format("%s is not a user", user));
        }
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, parentsUUIDs));
        List<Party> parents = userService.find(params);
        userService.removeParents((User) user, new HashSet<>(parents));
    }
}
