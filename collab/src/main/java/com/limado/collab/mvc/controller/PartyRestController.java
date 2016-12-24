/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.controller;

import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import com.limado.collab.mvc.exception.ResourceNotFoundException;
import com.limado.collab.service.GroupService;
import com.limado.collab.service.OrganizationService;
import com.limado.collab.service.PartyService;
import com.limado.collab.service.UserService;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@RestController
@RequestMapping("/api/v1/parties")
public class PartyRestController {

    private static final Logger log = LogManager.getLogger(PartyRestController.class);

    @Autowired
    private PartyService<Party> partyService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;

    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Party getById(@PathVariable String id, @RequestParam(name = QueryParams.Q_FETCH_RELATIONS, required = false) String fetchRelations) {
        log.debug("getById: " + id + " , fetchRelations = " + fetchRelations);
        UUID uuid = UUID.fromString(id);
        Party party;
        try {
            if (fetchRelations != null) {
                party = partyService.getById(uuid, fetchRelations.split(","));
            } else {
                party = partyService.getById(uuid);
            }
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("party uuid is not exist", e, id);
        }
        return party;
    }

    @GetMapping(value = "{id}/parents", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Set<Party> getParents(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        Set<Party> parents = partyService.getParents(uuid);
        removePartyRelations(parents);
        return parents;
    }

    @GetMapping(value = "{id}/children", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Set<Party> getChildren(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);
        Set<Party> children = partyService.getChildren(uuid);
        removePartyRelations(children);
        return children;
    }

    @GetMapping(value = "{id}/ascendants", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object getAscendants(@PathVariable String id, @RequestParam() Map<String, String> requestParam) {
        UUID uuid = UUID.fromString(id);
        Set<Party> ascendants = partyService.getAscendants(uuid);
        if(requestParam != null && !requestParam.isEmpty()) {
            QueryParams params = new QueryParams();
            params.putAll(requestParam);
            List<Party> parties = partyService.find(params);
            parties.retainAll(ascendants);
            if (params.isOnlySize()) {
                return parties.size();
            }
            else {
                return parties;
            }
        }
        return ascendants;
    }

    @GetMapping(value = "{id}/descendants", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object getDescendants(@PathVariable String id, @RequestParam() Map<String, String> requestParam) {
        UUID uuid = UUID.fromString(id);
        Set<Party> descendants = partyService.getDescendants(uuid);
        if(requestParam != null && !requestParam.isEmpty()) {
            QueryParams params = new QueryParams();
            params.putAll(requestParam);
            List<Party> parties = partyService.find(params);
            parties.retainAll(descendants);
            if (params.isOnlySize()) {
                return parties.size();
            }
            else {
                return parties;
            }
        }
        return descendants;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object find(@RequestParam() Map<String, String> requestParam) {
        log.debug("find requestParam: " + requestParam);
        QueryParams params = new QueryParams();
        params.putAll(requestParam);
        if (params.isOnlySize()) {
            return partyService.findSize(params);
        } else {
            List<Party> parties = partyService.find(params);
            return parties;
        }
    }

    @PutMapping("enable")
    public void enable(@RequestBody List<String> idList) {
        log.debug("enable: " + idList);
        Set<UUID> uuids = idList.stream().map(UUID::fromString).collect(Collectors.toSet());
        partyService.enable(uuids);
    }

    @PutMapping("disable")
    public void disable(@RequestBody List<String> idList) {
        log.debug("disable: " + idList);
        Set<UUID> uuids = idList.stream().map(UUID::fromString).collect(Collectors.toSet());
        partyService.disable(uuids);
    }

    @DeleteMapping
    public void deleteByIds(@RequestBody List<String> idList) {
        log.debug("delete: " + idList);
        Set<UUID> uuids = idList.stream().map(UUID::fromString).collect(Collectors.toSet());

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, uuids));
        List<Party> parties = partyService.find(params);
        Map<String, List<Party>> partyTypeMap = parties.stream().collect(Collectors.groupingBy(Party::getType));
        if(partyTypeMap.get(Group.TYPE) != null) {
            partyTypeMap.get(Group.TYPE).forEach(party -> groupService.delete((Group)party));
        }
        if(partyTypeMap.get(Organization.TYPE) != null) {
            partyTypeMap.get(Organization.TYPE).forEach(party -> organizationService.delete((Organization) party));
        }
        if(partyTypeMap.get(User.TYPE) != null) {
            partyTypeMap.get(User.TYPE).forEach(party -> userService.delete((User)party));
        }
    }

    // do not serialize relations
    private void removePartyRelations(Collection<Party> parties) {
        if(parties != null) {
            parties.forEach(party -> party.setParents(null));
            parties.forEach(party -> party.setChildren(null));
        }
    }
}
