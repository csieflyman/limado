/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.limado.collab.dao.IntervalTreeDao;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.commons.collections.CollectionUtils;
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
@Service
public class OrganizationServiceImpl extends PartyServiceImpl<Organization> implements OrganizationService {

    private static final Logger log = LogManager.getLogger(OrganizationServiceImpl.class);

    @Autowired
    @Qualifier("partyIntervalTreeDao")
    private IntervalTreeDao<UUID> intervalTreeDao;

    @Override
    public void movePartyToOrganization(Party child, Organization organization) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(organization != null, "organization must not be null");

        if(!child.getType().equals(Group.TYPE)) {
            child = getById(child.getId(), Party.RELATION_PARENT);
            Optional<Party> parentOrg = child.getParents().stream().filter(parent -> parent.getType().equals(Organization.TYPE)).findFirst();
            if(parentOrg.isPresent()) {
                intervalTreeDao.removeChild(parentOrg.get().getId(), child.getId());
                super.removeChild((Organization) parentOrg.get(), child);
            }
            super.addChild(organization, child);
            intervalTreeDao.addChild(organization.getId(), child.getId());
        }
        else {
            throw new IllegalArgumentException(String.format("organization %s can't add group child %s", organization, child));
        }
    }

    @Override
    public void addChild(Organization parent, Party child) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(child != null, "child must not be null");

        child = getById(child.getId(), Party.RELATION_PARENT);
        validateChildType(child);
        validateParentsRelationship(parent, child);

        super.addChild(parent, child);
        intervalTreeDao.addChild(parent.getId(), child.getId());
    }

    @Override
    public void removeChild(Organization parent, Party child) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(child != null, "child must not be null");

        intervalTreeDao.removeChild(parent.getId(), child.getId());
        super.removeChild(parent, child);
    }

    @Override
    public void addChildren(Organization parent, Set<Party> children) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(children != null, "children must not be null");

        if(children.isEmpty())
            return;

        children = loadChildren(children);
        for(Party child: children) {
            validateChildType(child);
            validateParentsRelationship(parent, child);
        }

        super.addChildren(parent, children);
        children.forEach(child -> intervalTreeDao.addChild(parent.getId(), child.getId()));
    }

    @Override
    public void removeChildren(Organization parent, Set<Party> children) {
        Preconditions.checkArgument(parent != null, "parent must not be null");
        Preconditions.checkArgument(children != null, "children must not be null");

        if(children.isEmpty())
            return;

        children.forEach(child -> intervalTreeDao.removeChild(parent.getId(), child.getId()));
        super.removeChildren(parent, children);
    }

    @Override
    public void addParents(Organization child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if(parents.isEmpty())
            return;

        Map<String, List<Party>> parentsTypeMap = parents.stream().collect(Collectors.groupingBy(Party::getType));
        if(parentsTypeMap.get(User.TYPE) != null) {
            throw new IllegalArgumentException(String.format("organization %s can't add user parent %s", child, parentsTypeMap.get(User.TYPE)));
        }

        else if(parentsTypeMap.get(Organization.TYPE) != null && parentsTypeMap.get(Organization.TYPE).size() > 1) {
            throw new IllegalArgumentException(String.format("organization %s can't add above two organization parents %s", child, parentsTypeMap.get(Organization.TYPE)));
        }

        super.addParents(child, parents);
        for(Party parent: parents) {
            if(parent.getType().equals(Organization.TYPE)) {
                intervalTreeDao.addChild(parent.getId(), child.getId());
            }
        }
    }

    @Override
    public void removeParents(Organization child, Set<Party> parents) {
        Preconditions.checkArgument(child != null, "child must not be null");
        Preconditions.checkArgument(parents != null, "parents must not be null");

        if(parents.isEmpty())
            return;

        for(Party parent: parents) {
            if(parent.getType().equals(Organization.TYPE)) {
                intervalTreeDao.removeChild(parent.getId(), child.getId());
            }
        }
        super.removeParents(child, parents);
    }

    @Override
    public void delete(Organization organization) {
        Preconditions.checkArgument(organization != null, "organization must not be null");

        intervalTreeDao.delete(organization.getId());
        super.delete(organization);
    }

    @Override
    public Set<Party> getDescendants(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        List<UUID> descendantIds = intervalTreeDao.getSubTree(id);
        if(descendantIds.isEmpty())
            return new HashSet<>();

        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, descendantIds));
        List<Party> parties = find(params);
        parties = parties.stream().sorted(Comparator.comparing(party -> descendantIds.indexOf(party.getId()))).collect(Collectors.toList());
        Set<Party> descendants = new LinkedHashSet<>(parties);
        return descendants;
    }

    private Set<Party> loadChildren(Set<Party> children) {
        Set<UUID> childrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, childrenIds));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_PARENT));
        children = new HashSet<>(find(params));
        if(children.size() != childrenIds.size()) {
            Set<UUID> foundChildrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
            throw new IllegalArgumentException(String.format("children id %s are not exist", CollectionUtils.subtract(childrenIds, foundChildrenIds)));
        }
        return children;
    }

    private void validateChildType(Party child) {
        if(child.getType().equals(Group.TYPE)) {
            throw new IllegalArgumentException(String.format("organization can't add group child %s", child.getId()));
        }
    }

    private void validateParentsRelationship(Organization newParent, Party child) {
        Optional<Party> parentOrg = child.getParents().stream().filter(parent -> parent.getType().equals(Organization.TYPE)).findFirst();
        if(parentOrg.isPresent()) {
            Party currentParent = parentOrg.get();
            if(currentParent.equals(newParent)) {
                throw new IllegalArgumentException(String.format("parent %s already has child %s", newParent, child));
            }
            else {
                throw new IllegalArgumentException(String.format("child %s can't have above two parents organization %s and %s", child, newParent, currentParent));
            }
        }
    }
}