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
            addChildren(newParty.getId(), children.stream().map(Party::getId).collect(Collectors.toSet()));
        }
        if(parents != null && !parents.isEmpty()) {
            addParents(newParty.getId(), parents.stream().map(Party::getId).collect(Collectors.toSet()));
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
                addChildren(party.getId(), new HashSet<>(addChildren).stream().map(Party::getId).collect(Collectors.toSet()));
            }
            if(!removeChildren.isEmpty()) {
                removeChildren(party.getId(), new HashSet<>(removeChildren).stream().map(Party::getId).collect(Collectors.toSet()));
            }
        }
        if(parents != null) {
            Collection<Party> addParents = CollectionUtils.subtract(parents, oldParty.getParents());
            Collection<Party> removeParents = CollectionUtils.subtract(oldParty.getParents(), parents);
            if(!addParents.isEmpty()) {
                addParents(party.getId(), new HashSet<>(addParents).stream().map(Party::getId).collect(Collectors.toSet()));
            }
            if(!removeParents.isEmpty()) {
                removeParents(party.getId(), new HashSet<>(removeParents).stream().map(Party::getId).collect(Collectors.toSet()));
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
    public void deleteById(UUID id) {
        Preconditions.checkArgument(id != null, "party id must not be null");

        Party party = getById(id, Party.RELATION_PARENT, Party.RELATION_CHILDREN);
        if(party.getChildren() != null && !party.getChildren().isEmpty()) {
            partyDao.removeChildren(party.getId(), party.getChildren().stream().map(Party::getId).collect(Collectors.toSet()));
        }
        if(party.getParents() != null && !party.getParents().isEmpty()) {
            partyDao.removeParents(party.getId(), party.getParents().stream().map(Party::getId).collect(Collectors.toSet()));
        }
        dagEdgeDao.removeEdgesOfVertex(id);
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
            if(party.getChildren() != null && !party.getChildren().isEmpty()) {
                partyDao.removeChildren(party.getId(), party.getChildren().stream().map(Party::getId).collect(Collectors.toSet()));
            }
            if(party.getParents() != null && !party.getParents().isEmpty()) {
                partyDao.removeParents(party.getId(), party.getParents().stream().map(Party::getId).collect(Collectors.toSet()));
            }
            dagEdgeDao.removeEdgesOfVertex(party.getId());
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
        Set<Party> ascendants = new HashSet<>(find(params));
        return ascendants;
    }

    @Override
    public Set<Party> getDescendants(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        Set descendantIds = dagEdgeDao.findOutgoingVertices(id);
        if(descendantIds.isEmpty())
            return new HashSet<>();
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, descendantIds));
        Set<Party> descendants = new HashSet<>(find(params));
        return descendants;
    }

    @Override
    public void addChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childId != null, "childId must not be null");

        partyDao.addChild(parentId, childId);
        dagEdgeDao.addEdges(parentId, childId);
    }

    @Override
    public void removeChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childId != null, "childId must not be null");

        dagEdgeDao.removeEdges(parentId, childId);
        partyDao.removeChild(parentId, childId);
    }

    @Override
    public void addChildren(UUID parentId, Set<UUID> childrenIds) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childrenIds != null, "childrenIds must not be null");

        if(childrenIds.isEmpty())
            return;
        partyDao.addChildren(parentId, childrenIds);
        childrenIds.forEach(childId -> dagEdgeDao.addEdges(parentId, childId));
    }

    @Override
    public void removeChildren(UUID parentId, Set<UUID> childrenIds) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childrenIds != null, "childrenIds must not be null");

        if (childrenIds.isEmpty())
            return;
        childrenIds.forEach(childId -> dagEdgeDao.removeEdges(parentId, childId));
        partyDao.removeChildren(parentId, childrenIds);
    }

    @Override
    public void addParents(UUID childId, Set<UUID> parentsIds) {
        Preconditions.checkArgument(childId != null, "childId must not be null");
        Preconditions.checkArgument(parentsIds != null, "parentsIds must not be null");

        if(parentsIds.isEmpty())
            return;
        partyDao.addParents(childId, parentsIds);
        parentsIds.forEach(parentId -> dagEdgeDao.addEdges(parentId, childId));
    }

    @Override
    public void removeParents(UUID childId, Set<UUID> parentsIds) {
        Preconditions.checkArgument(childId != null, "childId must not be null");
        Preconditions.checkArgument(parentsIds != null, "parentsIds must not be null");

        if(parentsIds.isEmpty())
            return;
        parentsIds.forEach(parentId -> dagEdgeDao.removeEdges(parentId, childId));
        partyDao.removeParents(childId, parentsIds);
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
