/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.controller;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.limado.collab.model.Party;
import com.limado.collab.mvc.exception.BadRequestException;
import com.limado.collab.mvc.exception.ResourceNotFoundException;
import com.limado.collab.service.PartyService;
import com.limado.collab.util.converter.json.JsonView;
import com.limado.collab.util.converter.json.Match;
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

    @GetMapping(value = "{uuidString}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object getById(@PathVariable String uuidString, @RequestParam(name=QueryParams.Q_FETCH_RELATIONS, required=false) String fetchRelations,
                            @RequestParam(name=QueryParams.Q_FETCH_PROPERTIES, required=false) String fetchProperties) {
        log.debug("getById: " + uuidString + " , fetchRelations = " + fetchRelations + ", fetchProperties = " + fetchProperties);
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        }catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid uuid format", e, uuidString);
        }

        Party party;
        try {
            if (fetchRelations != null) {
                party = partyService.getById(uuid, fetchRelations.split(","));
                if(fetchRelations.contains(Party.RELATION_PARENT)) {
                    removePartyRelations(party.getParents());
                }
                if(fetchRelations.contains(Party.RELATION_CHILDREN)) {
                    removePartyRelations(party.getChildren());
                }
            } else {
                party = partyService.getById(uuid);
            }
        } catch(IllegalArgumentException e) {
            throw new ResourceNotFoundException("party uuid is not exist", e, uuidString);
        }

        if (fetchProperties != null) {
            return JsonView.with(party).onClass(Party.class, Match.match().exclude("*").include(Sets.newHashSet(Splitter.on(",").split(fetchProperties))));
        } else {
            return party;
        }
    }

    @GetMapping(value ="{uuidString}/parents", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object getParents(@PathVariable String uuidString) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        }catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid uuid format", e, uuidString);
        }
        Set<Party> parents = partyService.getParents(uuid);
        removePartyRelations(parents);
        return parents;
    }

    @GetMapping(value ="{uuidString}/children", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object getChildren(@PathVariable String uuidString) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        }catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid uuid format", e, uuidString);
        }
        Set<Party> children = partyService.getChildren(uuid);
        removePartyRelations(children);
        return children;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object find(@RequestParam() Map<String, String> requestParam) {
        log.debug("find requestParam: " + requestParam);
        QueryParams params = parseRequestParamToQueryParams(requestParam);
        if(params.isOnlySize()) {
            return partyService.findSize(params);
        }
        else {
            List<Party> parties = partyService.find(params);

            if(!params.getFetchRelations().isEmpty()) {
                parties.forEach(party -> {
                    if (params.getFetchRelations().contains(Party.RELATION_PARENT)) {
                        removePartyRelations(party.getParents());
                    }
                    if (params.getFetchRelations().contains(Party.RELATION_CHILDREN)) {
                        removePartyRelations(party.getChildren());
                    }
                });
            }

            if (!params.getFetchProperties().isEmpty()) {
                return JsonView.with(parties).onClass(Party.class, Match.match().exclude("*").include(params.getFetchProperties()));
            } else {
                return parties;
            }
        }
    }

    @PutMapping("enable")
    public void enable(@RequestBody List<String> uuidStringList) {
        log.debug("enable: " + uuidStringList);
        List<UUID> uuids;
        try {
            uuids = uuidStringList.stream().map(UUID::fromString).collect(Collectors.toList());
        }catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid uuid format", e, uuidStringList);
        }
        partyService.enable(uuids);
    }

    @PutMapping("disable")
    public void disable(@RequestBody List<String> uuidStringList) {
        log.debug("disable: " + uuidStringList);
        List<UUID> uuids;
        try {
            uuids = uuidStringList.stream().map(UUID::fromString).collect(Collectors.toList());
        }catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid uuid format", e, uuidStringList);
        }
        partyService.disable(uuids);
    }

    @DeleteMapping
    public void deleteByIds(@RequestBody List<String> uuidStringList) {
        log.debug("delete: " + uuidStringList);
        List<UUID> uuids;
        try {
            uuids = uuidStringList.stream().map(UUID::fromString).collect(Collectors.toList());
        }catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid uuid format", e, uuidStringList);
        }
        partyService.deleteByIds(uuids);
    }

    private QueryParams parseRequestParamToQueryParams(Map<String, String> requestParam) {
        QueryParams params = new QueryParams();

        if(requestParam == null)
            return params;

        if(requestParam.get(QueryParams.Q_OFFSET) != null) {
            params.put(QueryParams.Q_OFFSET, requestParam.get(QueryParams.Q_OFFSET) );
        }
        if(requestParam.get(QueryParams.Q_LIMIT) != null) {
            params.put(QueryParams.Q_LIMIT, requestParam.get(QueryParams.Q_LIMIT) );
        }
        if(requestParam.get(QueryParams.Q_SORT) != null) {
            params.put(QueryParams.Q_SORT, requestParam.get(QueryParams.Q_SORT) );
        }
        if(requestParam.get(QueryParams.Q_ONLY_SIZE) != null) {
            params.put(QueryParams.Q_ONLY_SIZE, requestParam.get(QueryParams.Q_ONLY_SIZE) );
        }
        if(requestParam.get(QueryParams.Q_PREDICATES) != null) {
            params.put(QueryParams.Q_PREDICATES, requestParam.get(QueryParams.Q_PREDICATES) );
        }
        if(requestParam.get(QueryParams.Q_PREDICATES_DISJUNCTION) != null) {
            params.put(QueryParams.Q_PREDICATES_DISJUNCTION, requestParam.get(QueryParams.Q_PREDICATES_DISJUNCTION) );
        }
        if(requestParam.get(QueryParams.Q_FETCH_PROPERTIES) != null) {
            params.put(QueryParams.Q_FETCH_PROPERTIES, requestParam.get(QueryParams.Q_FETCH_PROPERTIES) );
        }
        if(requestParam.get(QueryParams.Q_FETCH_RELATIONS) != null) {
            params.put(QueryParams.Q_FETCH_RELATIONS, requestParam.get(QueryParams.Q_FETCH_RELATIONS) );
        }

        return params;
    }

    // avoid StackOverFlow problem when serialize party relations recursively
    private void removePartyRelations(Collection<Party> parties) {
        if(parties != null) {
            parties.forEach(party -> party.setParents(null));
            parties.forEach(party -> party.setChildren(null));
        }
    }
}
