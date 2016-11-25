/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.controller;

import com.limado.collab.model.Group;
import com.limado.collab.mvc.exception.BadRequestException;
import com.limado.collab.mvc.form.GroupForm;
import com.limado.collab.mvc.validator.PartyFormValidator;
import com.limado.collab.mvc.validator.ValidationUtils;
import com.limado.collab.service.GroupService;
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

import java.util.UUID;

/**
 * author flyman
 */
@RestController
@RequestMapping("/api/v1/groups")
public class GroupRestController {

    private static final Logger log = LogManager.getLogger(GroupRestController.class);

    @Autowired
    private GroupService groupService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity create(@RequestBody @Validated GroupForm form, BindingResult result) {
        log.debug("create groupForm: " + form);
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid group data.", null, ValidationUtils.buildErrorMessage(result));
        }
        Group group = form.buildModel();
        group = groupService.create(group);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @PutMapping(value = "{uuidString}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void update(@PathVariable String uuidString, @RequestBody @Validated GroupForm form, BindingResult result) {
        log.debug("update groupForm: " + form);
        if(!form.getId().toString().equals(uuidString)) {
            throw new BadRequestException("invalid uuid.", null, String.format("path uuid %s isn't the same as uuid %s in request body", uuidString, form.getId()));
        }
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid group data.", null, ValidationUtils.buildErrorMessage(result));
        }
        Group group = form.buildModel();
        groupService.update(group);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(new PartyFormValidator());
    }

    @PostMapping("{parentId}/child/{childId}")
    public void addChild(@PathVariable String parentId, @PathVariable String childId) {
        UUID parentUUID;
        UUID childUUID;
        try {
            parentUUID = UUID.fromString(parentId);
            childUUID = UUID.fromString(childId);
        }catch (IllegalArgumentException e) {
            throw new BadRequestException(String.format("invalid uuid format: %s, %s", parentId, childId) , e);
        }
        groupService.addChild(parentUUID, childUUID);
    }

    @DeleteMapping("{parentId}/child/{childId}")
    public void removeChild(@PathVariable String parentId, @PathVariable String childId) {
        UUID parentUUID;
        UUID childUUID;
        try {
            parentUUID = UUID.fromString(parentId);
            childUUID = UUID.fromString(childId);
        }catch (IllegalArgumentException e) {
            throw new BadRequestException(String.format("invalid uuid format: %s, %s", parentId, childId) , e);
        }
        groupService.removeChild(parentUUID, childUUID);
    }
}
