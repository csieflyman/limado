/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.util.query.Operator;
import com.limado.collab.util.query.Predicate;
import com.limado.collab.util.query.QueryParams;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@Service
public class OrganizationServiceImpl extends PartyServiceImpl<Organization> implements OrganizationService {

    @Override
    public void movePartyToOrganization(UUID childId, UUID organizationId) {
        Preconditions.checkArgument(childId != null, "organization must not be null");
        Preconditions.checkArgument(organizationId != null, "to organization must not be null");

        Party child = getById(childId, Party.RELATION_PARENT);
        if(!child.getType().equals(Group.TYPE)) {
            Optional<Party> parentOrg = child.getParents().stream().filter(parent -> parent.getType().equals(Organization.TYPE)).findFirst();
            if(parentOrg.isPresent()) {
                super.removeChild(parentOrg.get().getId(), childId);
            }
            super.addChild(organizationId, childId);
        }
        else {
            throw new IllegalArgumentException(String.format("organization %s can't add group child %s", organizationId, childId));
        }
    }

    @Override
    public void addChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childId != null, "childId must not be null");

        Party child = getById(childId, Party.RELATION_PARENT);
        validateChildType(child);
        validateParentsRelationship(parentId, child);

        super.addChild(parentId, childId);
    }

    @Override
    public void addChildren(UUID parentId, Set<UUID> childrenIds) {
        Preconditions.checkArgument(parentId != null, "parentId must not be null");
        Preconditions.checkArgument(childrenIds != null, "childrenIds must not be null");

        if(childrenIds.isEmpty())
            return;

        Set<Party> children = loadChildren(childrenIds);
        for(Party child: children) {
            validateChildType(child);
            validateParentsRelationship(parentId, child);
        }

        super.addChildren(parentId, childrenIds);
    }

    @Override
    public void addParents(UUID childId, Set<UUID> parentsIds) {
        Preconditions.checkArgument(childId != null, "childId must not be null");
        Preconditions.checkArgument(parentsIds != null, "parentsIds must not be null");

        if(parentsIds.isEmpty())
            return;
        else if(parentsIds.size() > 1) {
            throw new UnsupportedOperationException(String.format("organization %s can't add above two parents", childId));
        }

        super.addParents(childId, parentsIds);
    }

    private Set<Party> loadChildren(Set<UUID> childrenIds) {
        QueryParams params = new QueryParams();
        params.addPredicate(new Predicate("id", Operator.IN, childrenIds));
        params.setFetchRelations(Sets.newHashSet(Party.RELATION_PARENT));
        Set<Party> children = new HashSet<>(find(params));
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

    private void validateParentsRelationship(UUID newParentId, Party child) {
        Optional<Party> parentOrg = child.getParents().stream().filter(parent -> parent.getType().equals(Organization.TYPE)).findFirst();
        if(parentOrg.isPresent()) {
            Party currentParent = parentOrg.get();
            if(currentParent.getId().equals(newParentId)) {
                throw new IllegalArgumentException(String.format("parent %s already has child %s", newParentId, child.getId()));
            }
            else {
                throw new IllegalArgumentException(String.format("child %s can't have above two parents organization %s and %s", child.getId(), newParentId, currentParent.getId()));
            }
        }
    }
}