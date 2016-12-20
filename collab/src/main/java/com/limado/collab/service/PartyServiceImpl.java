/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.limado.collab.dao.DagEdgeDao;
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

/**
 * author flyman
 */
@Service("partyService")
public class PartyServiceImpl<T extends Party> implements PartyService<T> {

    private static final Logger log = LogManager.getLogger(PartyServiceImpl.class);

    @Autowired
    @Qualifier("partyDao")
    private PartyDao partyDao;

    @Autowired
    @Qualifier("partyDagEdgeDao")
    private DagEdgeDao<UUID> dagEdgeDao;

    @Override
    public T create(T party) {
        Preconditions.checkArgument(party != null, "party must not be null");

        if(checkExist(party.getType(), party.getIdentity())) {
            throw new IllegalArgumentException(String.format("identity %s must be unique of the type %s", party.getIdentity(), party.getType()));
        }

        Set<Party> parents = party.getParents();
        Set<Party> children = party.getChildren();
        party.setParents(new HashSet<>());
        party.setChildren(new HashSet<>());
        T newParty = (T) partyDao.create(party);

        if(children != null && !children.isEmpty()) {
            addChildren(newParty, children);
        }
        if(parents != null && !parents.isEmpty()) {
            addParents(newParty, parents);
        }

        return newParty;
    }

    @Override
    public void update(T party) {
        Preconditions.checkArgument(party != null, "party must not be null");
        Preconditions.checkArgument(party.getId() != null, "party id must not be null");

        Party oldParty = getById(party.getId(), Party.RELATION_PARENT, Party.RELATION_CHILDREN);
        if(!party.getIdentity().equals(oldParty.getIdentity()) && checkExist(party.getType(), party.getIdentity())) {
            throw new IllegalArgumentException(String.format("identity %s must be unique of the type %s", party.getIdentity(), party.getType()));
        }

        Set<Party> parents = party.getParents();
        Set<Party> children = party.getChildren();
        party.setChildren(oldParty.getChildren());
        party.setParents(oldParty.getParents());
        partyDao.update(party);

        if(children != null) {
            Collection<Party> addChildren = CollectionUtils.subtract(children, oldParty.getChildren());
            Collection<Party> removeChildren = CollectionUtils.subtract(oldParty.getChildren(), children);
            if(!addChildren.isEmpty()) {
                party = get(party, Party.RELATION_CHILDREN);
                addChildren(party, new HashSet<>(addChildren));
            }
            if(!removeChildren.isEmpty()) {
                party = get(party, Party.RELATION_CHILDREN);
                removeChildren(party, new HashSet<>(removeChildren));
            }
        }
        if(parents != null) {
            Collection<Party> addParents = CollectionUtils.subtract(parents, oldParty.getParents());
            Collection<Party> removeParents = CollectionUtils.subtract(oldParty.getParents(), parents);
            if(!addParents.isEmpty()) {
                addParents(party, new HashSet<>(addParents));
            }
            if(!removeParents.isEmpty()) {
                removeParents(party, new HashSet<>(removeParents));
            }
        }
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
    public void delete(T party) {
        Preconditions.checkArgument(party != null, "party must not be null");

        party = (T) getById(party.getId(), Party.RELATION_PARENT, Party.RELATION_CHILDREN);
        if(party.getChildren() != null && !party.getChildren().isEmpty()) {
            partyDao.removeChildren(party, party.getChildren());
        }
        if(party.getParents() != null && !party.getParents().isEmpty()) {
            partyDao.removeParents(party, party.getParents());
        }
        dagEdgeDao.removeEdgesOfVertex(party.getId());
        partyDao.delete(party);
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

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("children.id", Operator.EQ, id));
        List<Party> parents = find(params);
        return new HashSet<>(parents);
    }

    @Override
    public Set<Party> getChildren(UUID id) {
        Preconditions.checkArgument(id != null, "party must not be null");

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("parents.id", Operator.EQ, id));
        List<Party> children = find(params);
        return new HashSet<>(children);
    }

    @Override
    public Set<Party> getAscendants(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        Set ascendantIds = dagEdgeDao.findIncomingVertices(id);
        if(ascendantIds.isEmpty())
            return new HashSet<>();
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, ascendantIds));
        return new HashSet<>(find(params));
    }

    @Override
    public Set<Party> getDescendants(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        Set descendantIds = dagEdgeDao.findOutgoingVertices(id);
        if(descendantIds.isEmpty())
            return new HashSet<>();
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, descendantIds));
        return new HashSet<>(find(params));
    }

    @Override
    public void addChild(T parent, Party child) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(child != null, "child must not be null");

        partyDao.addChild(parent, child);
        dagEdgeDao.addEdges(parent.getId(), child.getId());
    }

    @Override
    public void removeChild(T parent, Party child) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(child != null, "child must not be null");

        partyDao.removeChild(parent, child);
        dagEdgeDao.removeEdges(parent.getId(), child.getId());
    }

    @Override
    public void addChildren(T parent, Set<Party> children) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(children != null, "children must not be null");

        if(children.isEmpty())
            return;
        partyDao.addChildren(parent, children);
        children.forEach(child -> dagEdgeDao.addEdges(parent.getId(), child.getId()));
    }

    @Override
    public void removeChildren(T parent, Set<Party> children) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(children != null, "children must not be null");

        if(children.isEmpty())
            return;
        partyDao.removeChildren(parent, children);
        children.forEach(child -> dagEdgeDao.removeEdges(parent.getId(), child.getId()));
    }

    @Override
    public void addParents(T child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if(parents.isEmpty())
            return;
        partyDao.addParents(child, parents);
        parents.forEach(parent -> dagEdgeDao.addEdges(parent.getId(), child.getId()));
    }

    @Override
    public void removeParents(T child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if(parents.isEmpty())
            return;
        partyDao.removeParents(child, parents);
        parents.forEach(parent -> dagEdgeDao.removeEdges(parent.getId(), child.getId()));
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
