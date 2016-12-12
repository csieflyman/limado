/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.limado.collab.model.Party;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@Repository("partyDao")
public class PartyDaoImpl extends JpaGenericDaoImpl<Party, UUID> implements PartyDao {

    private static final Logger log = LogManager.getLogger(PartyDaoImpl.class);

    @Override
    public void addChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childId != null, "childId must not be null");

        Party parent = loadParent(parentId);
        Party child = getById(childId);
        if(parent.getChildren().contains(child)) {
            throw new IllegalArgumentException(String.format("%s is already a child of %s", childId, parentId));
        }
        parent.addChild(child);
        entityManager.merge(parent);
    }

    @Override
    public void removeChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childId != null, "childId must not be null");

        Party parent = loadParent(parentId);
        Party child = getById(childId);
        if(!parent.getChildren().contains(child)) {
            throw new IllegalArgumentException(String.format("%s is not a child of %s", childId, parentId));
        }
        parent.removeChild(child);
        entityManager.merge(parent);
    }

    @Override
    public void addChildren(UUID parentId, Set<UUID> childrenIds) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childrenIds != null, "childrenIds must not be null");

        if(childrenIds.isEmpty())
            return;

        Party parent = loadParent(parentId);
        Set<Party> children = loadChildren(childrenIds);
        if(CollectionUtils.containsAny(parent.getChildren(), children)) {
            throw new IllegalArgumentException(String.format("%s already contains some children %s", parentId, childrenIds));
        }
        children.forEach(child -> parent.addChild(child));
        entityManager.merge(parent);
    }

    @Override
    public void removeChildren(UUID parentId, Set<UUID> childrenIds) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childrenIds != null, "childrenIds must not be null");

        if(childrenIds.isEmpty())
            return;

        Party parent = loadParent(parentId);
        Set<Party> children = loadChildren(childrenIds);
        if(!CollectionUtils.isSubCollection(children, parent.getChildren())) {
            throw new IllegalArgumentException(String.format("%s doesn't contains some children %s", parentId, childrenIds));
        }
        children.forEach(child -> parent.removeChild(child));
        entityManager.merge(parent);
    }

    @Override
    public void addParents(UUID childId, Set<UUID> parentsIds) {
        Preconditions.checkArgument(childId != null, "childId must not be null");
        Preconditions.checkArgument(parentsIds != null, "parentsIds must not be null");

        if(parentsIds.isEmpty())
            return;

        Party child = getById(childId);
        Set<Party> parents = loadParents(parentsIds);
        for(Party parent: parents) {
            if(parent.getChildren().contains(child)) {
                throw new IllegalArgumentException(String.format("%s is already a child of %s", childId, parent.getId()));
            }
            parent.addChild(child);
            entityManager.merge(parent);
        }
    }

    @Override
    public void removeParents(UUID childId, Set<UUID> parentsIds) {
        Preconditions.checkArgument(childId != null, "childId must not be null");
        Preconditions.checkArgument(parentsIds != null, "parentsIds must not be null");

        if(parentsIds.isEmpty())
            return;

        Party child = getById(childId);
        Set<Party> parents = loadParents(parentsIds);
        for(Party parent: parents) {
            if(!parent.getChildren().contains(child)) {
                throw new IllegalArgumentException(String.format("%s is not a child of %s", childId, parent.getId()));
            }
            parent.removeChild(child);
            entityManager.merge(parent);
        }
    }

    private Party loadParent(UUID parentId) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.EQ, parentId));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_CHILDREN));
        List<Party> parties = find(params);
        Party parent = parties.isEmpty() ? null : parties.get(0);

        if(parent == null) {
            throw new IllegalArgumentException(String.format("party id %s is not exist", parentId));
        }
        return parent;
    }

    private Set<Party> loadParents(Set<UUID> parentsIds) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, parentsIds));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_CHILDREN));
        Set<Party> parents = new HashSet<>(find(params));
        if(parents.size() != parentsIds.size()) {
            Set<UUID> foundParentsIds = parents.stream().map(Party::getId).collect(Collectors.toSet());
            throw new IllegalArgumentException(String.format("parents id %s are not exist", CollectionUtils.subtract(parentsIds, foundParentsIds)));
        }
        return parents;
    }

    private Set<Party> loadChildren(Set<UUID> childrenIds) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, childrenIds));
        Set<Party> children = new HashSet<>(find(params));
        if(children.size() != childrenIds.size()) {
            Set<UUID> foundChildrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
            throw new IllegalArgumentException(String.format("children id %s are not exist", CollectionUtils.subtract(childrenIds, foundChildrenIds)));
        }
        return children;
    }
}