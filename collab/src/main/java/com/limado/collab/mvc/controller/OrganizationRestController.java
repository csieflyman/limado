/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.controller;

import com.limado.collab.model.Organization;
import com.limado.collab.mvc.exception.BadRequestException;
import com.limado.collab.mvc.form.OrganizationForm;
import com.limado.collab.mvc.validator.PartyFormValidator;
import com.limado.collab.mvc.validator.ValidationUtils;
import com.limado.collab.service.OrganizationService;
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
@RequestMapping("/api/v1/organizations")
public class OrganizationRestController {

    private static final Logger log = LogManager.getLogger(OrganizationRestController.class);

    @Autowired
    private OrganizationService organizationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity create(@RequestBody @Validated OrganizationForm form, BindingResult result) {
        log.debug("create orgForm: " + form);
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid org data.", null, ValidationUtils.buildErrorMessage(result));
        }
        Organization org = form.buildModel();
        org = organizationService.create(org);
        return ResponseEntity.status(HttpStatus.CREATED).body(org);
    }

    @PutMapping(value = "{uuidString}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void update(@PathVariable String uuidString, @RequestBody @Validated OrganizationForm form, BindingResult result) {
        log.debug("update orgForm: " + form);
        if(!form.getId().toString().equals(uuidString)) {
            throw new BadRequestException("invalid uuid.", null, String.format("path uuid %s isn't the same as uuid %s in request body", uuidString, form.getId()));
        }
        if(result.hasErrors()) {
            log.debug(ValidationUtils.buildErrorMessage(result));
            throw new BadRequestException("invalid org data.", null, ValidationUtils.buildErrorMessage(result));
        }
        Organization org = form.buildModel();
        organizationService.update(org);
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
        organizationService.addChild(parentUUID, childUUID);
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
        organizationService.removeChild(parentUUID, childUUID);
    }
}
