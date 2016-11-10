/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * author flyman
 */
@Service
public class OrganizationServiceImpl extends PartyServiceImpl<Organization> implements OrganizationService {

    @Override
    public void movePartyToOrganization(UUID childId, UUID organizationId) {
        Preconditions.checkArgument(childId != null, "organization must not be null");
        Preconditions.checkArgument(organizationId != null, "to organization must not be null");

        Party persistentChild = getById(childId, Party.RELATION_PARENT);
        if(!persistentChild.getType().equals(Group.TYPE)) {
            Optional<Party> parentOrg = persistentChild.getParents().stream().filter(parent -> parent.getType().equals(Organization.TYPE)).findFirst();
            if(parentOrg.isPresent()) {
                removeChild(parentOrg.get(), persistentChild);
            }

            Party persistentParent = getById(organizationId, Party.RELATION_CHILDREN);
            addChild(persistentParent, persistentChild);
        }
        else {
            throw new IllegalArgumentException(String.format("organization %s can't add group child %s", organizationId, childId));
        }
    }

    @Override
    public void addChild(UUID parentId, UUID childId) {
        Preconditions.checkArgument(parentId != null, "parent must not be null");
        Preconditions.checkArgument(childId != null, "child must not be null");

        Party child = getById(childId, Party.RELATION_PARENT);
        if(child.getType().equals(Group.TYPE)) {
            throw new IllegalArgumentException(String.format("organization %s can't add group child %s", parentId, childId));
        }

        Optional<Party> parentOrg = Optional.empty();
        if(child.getParents() != null) {
            parentOrg = child.getParents().stream().filter(parent -> parent.getType().equals(Organization.TYPE)).findFirst();
        }

        if(parentOrg.isPresent()) {
            if(parentOrg.get().getId().equals(parentId)) {
                throw new IllegalArgumentException(String.format("parent %s already has child %s", parentId, child));
            }
            else {
                throw new IllegalArgumentException(String.format("child %s can't have above two parents organization %s and %s", child, parentId, parentOrg.get().getId()));
            }
        }
        else {
            Party persistentParent = getById(parentId, Party.RELATION_CHILDREN);
            addChild(persistentParent, child);
        }
    }
}