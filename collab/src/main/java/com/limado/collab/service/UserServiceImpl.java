/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.service;

import com.google.common.base.Preconditions;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * author flyman
 */
@Service
public class UserServiceImpl extends PartyServiceImpl<User> implements UserService {

    @Override
    public void addChildren(UUID parentId, Set<UUID> childrenIds) {
        if(childrenIds.isEmpty())
            return;
        throw new UnsupportedOperationException(String.format("user %s can't add children", parentId));
    }

    @Override
    public void removeChildren(UUID parentId, Set<UUID> childrenIds) {
        if(childrenIds.isEmpty())
            return;
        throw new UnsupportedOperationException(String.format("user %s can't remove children", parentId));
    }

    @Override
    public void addChild(UUID parentId, UUID childId) {
        throw new UnsupportedOperationException(String.format("user %s can't add child", parentId));
    }

    @Override
    public void removeChild(UUID parentId, UUID childId) {
        throw new UnsupportedOperationException(String.format("user %s can't remove child", parentId));
    }

    @Override
    public Set<Party> getChildren(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        return new HashSet<>();
    }

    @Override
    public Set<Party> getDescendants(UUID id) {
        Preconditions.checkArgument(id != null, "id must not be null");

        return new HashSet<>();
    }
}
