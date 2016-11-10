/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.limado.collab.dao.PartyDao;
import com.limado.collab.model.Party;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@Service("partyService")
public class PartyServiceImpl<T extends Party> implements PartyService<T> {

    private static final Logger log = LogManager.getLogger(PartyServiceImpl.class);

    @Autowired
    @Qualifier("partyDao")
    private PartyDao partyDao;

    @Override
    public T create(T party) {
        Preconditions.checkArgument(party != null, "party must not be null");

        if(checkExist(party.getType(), party.getIdentity())) {
            throw new IllegalArgumentException(String.format("identity %s must be unique of the type %s", party.getIdentity(), party.getType()));
        }
        // no cascade for transient instance
        party.setParents(null);
        party.setChildren(null);
        return (T) partyDao.create(party);
    }

    @Override
    public void update(T party) {
        Preconditions.checkArgument(party != null, "party must not be null");
        Preconditions.checkArgument(party.getId() != null, "party id must not be null");

        Party persistentParty = getById(party.getId(), Party.RELATION_PARENT, Party.RELATION_CHILDREN);
        if(!party.getIdentity().equals(persistentParty.getIdentity())) {
            if(checkExist(party.getType(), party.getIdentity())) {
                throw new IllegalArgumentException(String.format("identity %s must be unique of the type %s", party.getIdentity(), party.getType()));
            }
        }

        if(party.getChildren() == null) {
            party.setChildren(persistentParty.getChildren());
        }
        else {
            Collection<Party> addChildren = CollectionUtils.subtract(party.getChildren(), persistentParty.getChildren());
            Collection<Party> removeChildren = CollectionUtils.subtract(persistentParty.getChildren(), party.getChildren());

            addChildren.forEach(child -> {
                Preconditions.checkArgument(child.getId() != null, String.format("child %s id must not be null", child));
                Party persistentChild = getById(child.getId(), Party.RELATION_PARENT);
                addChild(persistentParty, persistentChild);
            });

            removeChildren.forEach(persistentChild -> removeChild(persistentParty, persistentChild));
        }

        if(party.getParents() == null) {
            party.setParents(persistentParty.getParents());
        }
        else {
            Collection<Party> addParents = CollectionUtils.subtract(party.getParents(), persistentParty.getParents());
            Collection<Party> removeParents = CollectionUtils.subtract(persistentParty.getParents(), party.getParents());

            addParents.forEach(parent -> {
                Preconditions.checkArgument(parent.getId() != null, String.format("parent %s id must not be null", parent));
                Party persistentParent = getById(parent.getId(), Party.RELATION_CHILDREN);
                addChild(persistentParent, persistentParty);
            });

            removeParents.forEach(persistentParent -> removeChild(persistentParent, persistentParty));
        }

        partyDao.update(party);
    }

    @Override
    public T get(T party, String... relations) {
        return get(party, Sets.newHashSet(relations));
    }

    @Override
    public T get(T party, Collection<String> relations) {
        Preconditions.checkArgument(party != null, "party must not be null");

        if(party.getId() != null) {
            return (T) getById(party.getId(), relations);
        }
        else {
            return (T) getByTypeAndIdentity(party.getType(), party.getIdentity(), relations);
        }
    }

    @Override
    public Party getById(UUID id, String... relations) {
        return getById(id, Sets.newHashSet(relations));
    }

    @Override
    public Party getById(UUID id, Collection<String> relations) {
        Preconditions.checkArgument(id != null, "id must not be null");
        Party party;
        if(relations == null || relations.isEmpty()) {
            party = partyDao.getById(id);
        }
        else {
            QueryParams params = new QueryParams();
            params.addPredicate(new Predicate("id", Operator.EQ, id));
            params.setFetchRelations(Sets.newHashSet(relations));
            List<Party> parties = partyDao.find(params);
            party = parties.isEmpty() ? null : parties.get(0);
        }

        if(party == null) {
            throw new IllegalArgumentException(String.format("party id %s is not exist", id));
        }

        return party;
    }

    @Override
    public boolean checkExist(String type, String identity) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "party type must not be empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(identity), "party identity must not be empty");

        QueryParams params = new QueryParams();
        params.setOnlySize(true);
        params.addPredicate(new Predicate("type", Operator.EQ, type));
        params.addPredicate(new Predicate("identity", Operator.EQ, identity));
        int size = findSize(params);
        return size > 0;
    }

    @Override
    public void deleteById(UUID id) {
        Preconditions.checkArgument(id != null, "party id must not be null");

        Party party = getById(id, Party.RELATION_PARENT, Party.RELATION_CHILDREN);
        if(party.getChildren() != null) {
            party.getChildren().forEach(child -> removeChild(party, child));
        }
        if(party.getParents() != null) {
            party.getParents().forEach(parent -> removeChild(parent, party));
        }
        partyDao.delete(party);
    }

    @Override
    public void deleteByIds(Collection<UUID> ids) {
        Preconditions.checkArgument(ids != null, "party ids must not be null");

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, ids));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_PARENT, Party.RELATION_CHILDREN));
        List<Party> parties = find(params);
        for (Party party: parties) {
            if(party.getChildren() != null) {
                party.getChildren().forEach(child -> removeChild(party, child));
            }
            if(party.getParents() != null) {
                party.getParents().forEach(parent -> removeChild(parent, party));
            }
        }
        partyDao.batchDeleteById(parties.stream().map(Party::getId).collect(Collectors.toSet()));
    }

    @Override
    public List<Party> find(QueryParams queryParams) {
        Preconditions.checkArgument(queryParams != null, "queryParams must not be null");
        return partyDao.find(queryParams);
    }

    @Override
    public int findSize(QueryParams queryParams) {
        Preconditions.checkArgument(queryParams != null, "queryParams must not be null");

        return partyDao.findSize(queryParams);
    }

    @Override
    public void enable(Collection<UUID> ids) {
        Preconditions.checkArgument(ids != null, "party ids must not be null");

        Map<String, Object> updatedValueMap = new HashMap<>();
        updatedValueMap.put("enabled", true);
        partyDao.batchUpdate(ids, updatedValueMap);
    }

    @Override
    public void disable(Collection<UUID> ids) {
        Preconditions.checkArgument(ids != null, "party ids must not be null");

        Map<String, Object> updatedValueMap = new HashMap<>();
        updatedValueMap.put("enabled", false);
        partyDao.batchUpdate(ids, updatedValueMap);
    }

    @Override
    public Set<Party> getParents(UUID id) {
        Preconditions.checkArgument(id != null, "party must not be null");

        Party child = getById(id, Party.RELATION_PARENT);
        return child.getParents() == null ? Collections.emptySet() : child.getParents();
    }

    @Override
    public Set<Party> getChildren(UUID id) {
        Preconditions.checkArgument(id != null, "party must not be null");

        Party parent = getById(id, Party.RELATION_CHILDREN);
        return parent.getChildren() == null ? Collections.emptySet() : parent.getChildren();
    }

    @Override
    public void addChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childId != null, "childId must not be null");

        Party persistentParent = getById(parentId, Party.RELATION_CHILDREN);
        // hibernate will fetch child's parents to validate parent-child relationship
        Party persistentChild = getById(childId, Party.RELATION_PARENT);
        if(persistentParent.getChildren() != null && persistentParent.getChildren().contains(persistentChild)) {
            throw new IllegalArgumentException(String.format("child %s is already a child of %s", persistentChild, persistentParent));
        }

        addChild(persistentParent, persistentChild);
    }

    @Override
    public void removeChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childId != null, "childId must not be null");

        Party persistentParent = getById(parentId, Party.RELATION_CHILDREN);
        Party persistentChild = getById(childId, Party.RELATION_PARENT);
        if(persistentParent.getChildren() != null && !persistentParent.getChildren().contains(persistentChild)) {
            throw new IllegalArgumentException(String.format("%s is not a child of %s", persistentChild, persistentParent));
        }

        removeChild(persistentParent, persistentChild);
    }

    protected void addChild(Party persistentParent, Party persistentChild) {
        partyDao.addChild(persistentParent, persistentChild);
    }

    protected void removeChild(Party persistentParent, Party persistentChild) {
        partyDao.removeChild(persistentParent, persistentChild);
    }

    private Party getByTypeAndIdentity(String type, String identity, Collection<String> relations) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "party type must not be empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(identity), "party identity must not be empty");

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("type", Operator.EQ, type));
        params.addPredicate(new Predicate("identity", Operator.EQ, identity));
        if(relations != null && !relations.isEmpty()) {
            params.setFetchRelations(Sets.newHashSet(relations));
        }
        List<Party> parties = find(params);
        return parties.isEmpty() ? null : parties.get(0);
    }
}
